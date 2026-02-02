(ns leihs.inventory.server.resources.pool.items.filter-handler
  (:require
   [clojure.edn :as edn]
   [clojure.set :as set]
   [clojure.string :as str]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.fields.main :as fields]
   [taoensso.timbre :refer [error info]])
  (:import
   [java.sql Timestamp]
   [java.time LocalDate ZoneId]
   [java.util UUID]))

;; ------------------------------------------------------------
;; CONSTANTS
;; ------------------------------------------------------------
;; Filter uses MQL-style predicates (https://www.mongodb.com/docs/manual/reference/mql/query-predicates/)
;; Input: URL-encoded EDN string. Data flow: edn (fe) -> url-encoded string -> edn (be)

(def INVALID_FILTER "Invalid filter: must be valid EDN with allowed field names")
(def EDN_PARSE_ERROR "Invalid filter: could not parse EDN")

;; building_id is on rooms (items.room_id -> rooms.id, rooms.building_id); join rooms when filtering by it
(def ^:private uuid-fields
  #{:id :inventory_pool_id :owner_id :supplier_id :model_id :room_id :parent_id :building_id})

(def ^:private numeric-fields #{:price})

(def ^:private boolean-fields
  #{:is_inventory_relevant :is_broken :is_borrowable :is_incomplete :needs_permission})

;; retired is a date column (nil = not retired, date = when retired); filter accepts boolean: false => IS NULL, true => IS NOT NULL
(def ^:private retired-field :retired)

(def ^:private date-fields #{:last_check :invoice_date})

;; Item columns that can be used in filters (merged with API /fields for properties_* and custom fields).
;; building_id is resolved via join to rooms (rooms.building_id).
(def ^:private item-filter-columns
  #{:id :inventory_pool_id :owner_id :supplier_id :model_id :room_id :parent_id :building_id
    :last_check :invoice_date :retired :inventory_code :price
    :is_inventory_relevant :is_broken :is_borrowable :is_incomplete :needs_permission
    :insurance_number :item_version :responsible :serial_number :shelf :user_name
    :retired_reason :status_note :note :invoice_number :name})

;; ------------------------------------------------------------
;; UTILITIES
;; ------------------------------------------------------------

(defn extract-id-type [fields]
  (map (fn [field]
         {:id (if (string? (:id field)) (keyword (:id field)) (:id field))
          :type (:type field)
          :data_type (:data_type field)})
       fields))

(defn get-field-info [field fields-response*]
  (first (filter #(= (:id %) field) fields-response*)))

(defn- set-contains? [coll k]
  (and (set? coll) (contains? coll k)))

(defn- map-contains? [m k]
  (and (map? m) (contains? m k)))

(defn infer-data-type [field]
  (cond
    (set-contains? uuid-fields field) :uuid
    (set-contains? numeric-fields field) :numeric
    (set-contains? boolean-fields field) :boolean
    (set-contains? date-fields field) :date
    :else :text))

(defn get-fields-response [request]
  (-> (fields/index-resources
       (assoc-in request [:parameters :query :target_type] "item"))
      :body
      :fields
      extract-id-type))

;; ------------------------------------------------------------
;; EDN PARSING
;; ------------------------------------------------------------

(defn- try-parse-edn [filter-str]
  (edn/read-string filter-str))

(defn parse-filter-edn [filter-str]
  (when (or (nil? filter-str) (str/blank? filter-str))
    (throw (ex-info EDN_PARSE_ERROR {:filter filter-str :status 400})))
  (try
    (try-parse-edn filter-str)
    (catch Exception e
      ;; Work around truncation: "EOF while reading" often means missing closing brace(s)
      (let [msg (str (or (.getMessage e) ""))
            eof? (str/includes? msg "EOF")]
        (if eof?
          (let [open (count (filter #(= % \{) filter-str))
                close (count (filter #(= % \}) filter-str))
                missing (max 0 (- open close))
                repaired (when (and (pos? missing) (<= missing 2))
                           (str filter-str (str/join (repeat missing "}"))))]
            (if repaired
              (try (try-parse-edn repaired)
                   (catch Exception _e2
                     (throw (ex-info EDN_PARSE_ERROR {:filter filter-str :cause msg :status 400}))))
              (throw (ex-info EDN_PARSE_ERROR {:filter filter-str :cause msg :status 400}))))
          (throw (ex-info EDN_PARSE_ERROR {:filter filter-str :cause msg :status 400})))))))

(defn allowed-filter-fields [fields-response*]
  (let [field-ids (set (when (sequential? fields-response*) (map :id fields-response*)))
        ;; Allow properties_<key> when <key> is a defined field id that does not already have the prefix.
        property-prefixed (set (for [f field-ids
                                     :let [n (name f)]
                                     :when (not (str/starts-with? n "properties_"))]
                                 (keyword (str "properties_" n))))]
    (set/union item-filter-columns field-ids property-prefixed)))

(defn validate-field [field fields-response*]
  (let [field-kw (if (keyword? field) field (keyword (name field)))
        allowed (allowed-filter-fields fields-response*)
        allowed? (set-contains? allowed field-kw)]
    (when-not allowed?
      (throw (ex-info INVALID_FILTER {:field (name field-kw) :status 400})))
    field-kw))

;; ------------------------------------------------------------
;; DATE HANDLING
;; ------------------------------------------------------------

(defn parse-date-value [value]
  (cond
    (string? value) (LocalDate/parse value)
    (instance? java.time.LocalDate value) value
    :else (LocalDate/parse (str value))))

(defn local-date-to-timestamp [local-date]
  (-> local-date
      (.atStartOfDay (ZoneId/of "UTC"))
      .toInstant
      Timestamp/from))

;; ------------------------------------------------------------
;; MQL EDN -> SQL WHERE
;; ------------------------------------------------------------

(defn parse-value-by-type [value data-type]
  (try
    (case data-type
      :uuid (if (string? value) (UUID/fromString value) value)
      :integer (if (string? value) (Integer/parseInt value) value)
      :bigint (if (string? value) (Long/parseLong value) value)
      :decimal (if (string? value) (bigdec value) value)
      :numeric (if (string? value) (bigdec value) value)
      :boolean (if (string? value) (Boolean/parseBoolean value) value)
      :date (parse-date-value value)
      value)
    (catch Exception _ value)))

(defn- field-sql-expr
  "Return HoneySQL expression for a filter field. Qualify with items/rs so WHERE works when
   the query joins items, rooms (as rs), models, etc. (avoids ambiguous column / invalid table ref).
   Use [:raw ...] so we emit exact table.column in SQL (items.* and rs.building_id)."
  [field]
  (cond
    (str/starts-with? (name field) "properties_")
    (let [key (subs (name field) (count "properties_"))
          escaped (str/replace key #"'" "''")]
      [:raw (str "items.properties->>'" escaped "'")])
    ;; building_id is on rooms; items list uses alias rs for rooms. Raw so we never emit \"rooms\"
    (= field :building_id)
    [:raw "rs.building_id"]
    ;; item columns: qualify as items.<col> (field is validated so safe to interpolate)
    :else
    [:raw (str "items." (name field))]))

(defn mql-predicate->sql
  "Convert a single MQL-style predicate for one field into HoneySQL WHERE clause.
   pred can be: scalar value (implies $eq), or map like {:$eq v} {:$gte v} {:$lte v} {:$eq nil} etc."
  [field pred fields-response* data-type]
  (let [field-kw (validate-field field fields-response*)
        dtype (or data-type (infer-data-type field-kw))
        sql-field (field-sql-expr field-kw)]
    (cond
      ;; retired is date column; boolean in filter: false => not retired (IS NULL), true => retired (IS NOT NULL)
      (and (= field-kw retired-field) (or (true? pred) (false? pred)))
      (if pred [:is-not sql-field nil] [:is sql-field nil])

      ;; Simple nil value => IS NULL (for null checks, use {:field nil})
      (nil? pred)
      [:is sql-field nil]

      ;; Simple value => $eq
      (not (map? pred))
      (let [parsed (parse-value-by-type pred dtype)]
        (if (= dtype :date)
          (let [d (parse-date-value pred)
                end (.plusDays d 1)]
            [:between sql-field (local-date-to-timestamp d) (local-date-to-timestamp end)])
          [:= sql-field parsed]))

      ;; retired + $eq boolean (map form): {:retired {:$eq false}} => IS NULL, {:$eq true} => IS NOT NULL
      (and (= field-kw retired-field) (map-contains? pred :$eq))
      (let [v (get pred :$eq)]
        (if (true? v) [:is-not sql-field nil] [:is sql-field nil]))

      ;; $eq nil => IS NULL (for null checks, use {:field {:$eq nil}} or {:field nil})
      (and (map? pred) (map-contains? pred :$eq) (nil? (get pred :$eq)))
      [:is sql-field nil]

      ;; $ilike (partial match, case-insensitive; for text/textarea fields)
      (and (map? pred) (map-contains? pred :$ilike))
      (let [pattern (get pred :$ilike)
            pattern-str (str "%" pattern "%")]
        [:ilike sql-field pattern-str])

      ;; Date range: multiple comparisons in one map, e.g. {:$gte "2020-01-01" :$lte "2020-12-31"}
      ;; Must be before single comparison branch so we don't lose the second op
      (and (= dtype :date) (map? pred) (some #(get pred %) [:$gte :$lte]))
      (let [gte (some-> (get pred :$gte) parse-date-value local-date-to-timestamp)
            lte (when-let [d (some-> (get pred :$lte) parse-date-value)]
                  (local-date-to-timestamp (.plusDays d 1)))]
        (cond
          (and gte lte) [:between sql-field gte lte]
          gte [:>= sql-field gte]
          lte [:<= sql-field lte]
          :else (throw (ex-info "Invalid date predicate" {:field field :pred pred :status 400}))))

      ;; Single comparison operator: {:$eq v} {:$gte v} {:$lte v} (pred must be map)
      ;; Note: $ne, $gt, $lt removed - not required by UI (see TODO.md)
      (and (map? pred) (some #(get pred %) [:$eq :$gte :$lte]))
      (let [op (some #(when (get pred %) %) [:$eq :$gte :$lte])
            v (get pred op)
            parsed (parse-value-by-type v dtype)
            op->sql {:$eq := :$gte :>= :$lte :<=}]
        (when-not op
          (throw (ex-info "Invalid MQL predicate" {:field field :pred pred :status 400})))
        (if (= dtype :date)
          (let [ts (local-date-to-timestamp (parse-date-value v))]
            [(get op->sql op) sql-field ts])
          [(get op->sql op) sql-field parsed]))

      :else
      (throw (ex-info "Unsupported MQL predicate" {:field field :pred pred :status 400})))))

(defn mql-edn->where-clause
  "Convert MQL-style EDN filter to HoneySQL WHERE clause.
   Top-level can be: {:$and [...]} {:$or [...]} or a single field predicate map."
  [edn fields-response*]
  (when (string? edn)
    (throw (ex-info INVALID_FILTER {:edn edn :hint "top-level filter must be a map" :status 400})))
  (when (and (some? edn) (not (map? edn)))
    (throw (ex-info INVALID_FILTER {:edn (str edn) :hint "top-level filter must be a map" :status 400})))
  (cond
    (nil? edn)
    nil

    (empty? edn)
    nil

    ;; {:$or [pred1 pred2 ...]}
    (and (map? edn) (map-contains? edn :$or))
    (let [clauses (get edn :$or)
          clauses (if (sequential? clauses) clauses [clauses])
          sql-clauses (map #(mql-edn->where-clause % fields-response*) clauses)]
      (when (seq sql-clauses)
        (if (= (count sql-clauses) 1)
          (first sql-clauses)
          (vec (cons :or sql-clauses)))))

    ;; {:$and [pred1 pred2 ...]}
    (and (map? edn) (map-contains? edn :$and))
    (let [clauses (get edn :$and)
          clauses (if (sequential? clauses) clauses [clauses])
          sql-clauses (map #(mql-edn->where-clause % fields-response*) clauses)]
      (when (seq sql-clauses)
        (if (= (count sql-clauses) 1)
          (first sql-clauses)
          (vec (cons :and sql-clauses)))))

    ;; Single field predicate: {:field value} or {:field {:$op value}}
    (and (map? edn) (= (count edn) 1))
    (let [[field pred] (first edn)
          field-kw (validate-field field fields-response*)
          field-info (get-field-info field-kw fields-response*)
          dtype (or (:data_type field-info) (infer-data-type field-kw))]
      (mql-predicate->sql field pred fields-response* dtype))

    ;; Multiple fields at top level => implicit $and
    (map? edn)
    (let [clauses (map (fn [[f p]] (mql-edn->where-clause {f p} fields-response*)) edn)
          clauses (filter some? clauses)]
      (when (seq clauses)
        (if (= (count clauses) 1)
          (first clauses)
          (vec (cons :and clauses)))))

    :else
    (throw (ex-info INVALID_FILTER {:edn edn :status 400}))))

;; ------------------------------------------------------------
;; FILTER QUERY BUILDER
;; ------------------------------------------------------------

(defn create-filter-query-and-validate! [query request filter-str]
  (let [edn (parse-filter-edn filter-str)
        fields-response* (extract-id-type (get-fields-response request))
        conditions (mql-edn->where-clause edn fields-response*)]
    (if (seq conditions)
      (sql/where query conditions)
      query)))
