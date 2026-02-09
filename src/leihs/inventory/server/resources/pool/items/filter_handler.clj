(ns leihs.inventory.server.resources.pool.items.filter-handler
  (:require
   [clojure.edn :as edn]
   [clojure.set :as set]
   [clojure.string :as str]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.fields.main :as fields]
   [taoensso.timbre :refer [error info warn]])
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
(def RETIRED_INVALID_PRED "The :retired field only supports boolean predicates: {:retired true/false} or {:retired {:$eq true/false}}.")

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

(defn- extract-id-type [fields]
  (map (fn [field]
         {:id (if (string? (:id field)) (keyword (:id field)) (:id field))
          :type (:type field)
          :data_type (:data_type field)})
       fields))

(defn- get-field-info [field fields-response*]
  (first (filter #(= (:id %) field) fields-response*)))

(defn- infer-data-type [field]
  (cond
    (contains? uuid-fields field) :uuid
    (contains? numeric-fields field) :numeric
    (contains? boolean-fields field) :boolean
    (contains? date-fields field) :date
    :else :text))

(defn- get-fields-response [request]
  (-> (fields/index-resources
       (assoc-in request [:parameters :query :target_type] "item"))
      :body
      :fields
      extract-id-type))

;; ------------------------------------------------------------
;; EDN PARSING
;; ------------------------------------------------------------

(defn- parse-filter-edn [filter-str]
  (when (or (nil? filter-str) (str/blank? filter-str))
    (throw (ex-info EDN_PARSE_ERROR {:filter filter-str :status 400})))
  (try
    (edn/read-string filter-str)
    (catch Exception e
      (throw (ex-info EDN_PARSE_ERROR {:filter filter-str :cause (.getMessage e) :status 400})))))

(defn- allowed-filter-fields [fields-response*]
  (let [field-ids (set (when (sequential? fields-response*) (map :id fields-response*)))
        ;; Allow properties_<key> when <key> is a defined field id that does not already have the prefix.
        property-prefixed (set (for [f field-ids
                                     :let [n (name f)]
                                     :when (not (str/starts-with? n "properties_"))]
                                 (keyword (str "properties_" n))))]
    (set/union item-filter-columns field-ids property-prefixed)))

(defn- validate-field [field fields-response*]
  (let [field-kw (if (keyword? field) field (keyword (name field)))
        allowed (allowed-filter-fields fields-response*)]
    (when-not (contains? allowed field-kw)
      (throw (ex-info INVALID_FILTER {:field (name field-kw) :status 400})))
    field-kw))

;; ------------------------------------------------------------
;; DATE HANDLING
;; ------------------------------------------------------------

(defn- parse-date-value [value]
  (cond
    (string? value) (LocalDate/parse value)
    (instance? java.time.LocalDate value) value
    :else (LocalDate/parse (str value))))

(defn- local-date-to-timestamp [local-date]
  (-> local-date
      (.atStartOfDay (ZoneId/of "UTC"))
      .toInstant
      Timestamp/from))

;; ------------------------------------------------------------
;; MQL EDN -> SQL WHERE
;; ------------------------------------------------------------

(defn- parse-value-by-type [value data-type]
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
  "Return HoneySQL expression for a filter field. Qualify with table aliases so WHERE works
   when the query joins items, rooms, models, etc. (avoids ambiguous column / invalid table ref).
   table-aliases: {:rooms \"rs\"} — maps logical table to alias used in the query."
  [field table-aliases]
  (cond
    (str/starts-with? (name field) "properties_")
    ;; JSONB text extraction: use HoneySQL v2 operator syntax so the key becomes
    ;; a JDBC prepared-statement parameter (immune to SQL injection).
    ;; Produces: items.properties ->> ?  with key bound as parameter.
    (let [key (subs (name field) (count "properties_"))]
      [:->> :items.properties key])
    ;; building_id is on rooms; use the alias passed from the caller query
    (= field :building_id)
    [:raw (str (:rooms table-aliases) ".building_id")]
    ;; item columns: qualify as items.<col> (field is validated so safe to interpolate)
    :else
    [:raw (str "items." (name field))]))

(defn- mql-predicate->sql
  "Convert a single MQL-style predicate for one field into HoneySQL WHERE clause.
   pred can be: scalar value (implies $eq), or map like {:$eq v} {:$gte v} {:$lte v} {:$eq nil} etc.
   field-kw is already validated by caller (mql-edn->where-clause)."
  [field-kw pred data-type table-aliases]
  (let [dtype (or data-type (infer-data-type field-kw))
        sql-field (field-sql-expr field-kw table-aliases)]
    (cond
      ;; retired is a date column (nil = not retired, date = when retired).
      ;; Only boolean predicates are supported: true => IS NOT NULL, false => IS NULL.
      (and (= field-kw retired-field) (boolean? pred))
      (if pred [:is-not sql-field nil] [:is sql-field nil])

      (and (= field-kw retired-field) (map? pred) (contains? pred :$eq) (boolean? (get pred :$eq)))
      (if (get pred :$eq) [:is-not sql-field nil] [:is sql-field nil])

      ;; Reject any non-boolean predicate on retired (date operators, $ilike, etc.)
      (= field-kw retired-field)
      (throw (ex-info RETIRED_INVALID_PRED {:field "retired" :pred pred :status 400}))

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

      ;; $eq nil => IS NULL (for null checks, use {:field {:$eq nil}} or {:field nil})
      (and (map? pred) (contains? pred :$eq) (nil? (get pred :$eq)))
      [:is sql-field nil]

      ;; $ilike (partial match, case-insensitive; for text/textarea fields)
      (and (map? pred) (contains? pred :$ilike))
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
          :else (throw (ex-info "Invalid date predicate" {:field field-kw :pred pred :status 400}))))

      ;; Single comparison operator: {:$eq v} {:$gte v} {:$lte v} (pred must be map)
      (and (map? pred) (some #(get pred %) [:$eq :$gte :$lte]))
      (let [op (some #(when (get pred %) %) [:$eq :$gte :$lte])
            v (get pred op)
            parsed (parse-value-by-type v dtype)
            op->sql {:$eq := :$gte :>= :$lte :<=}]
        (if (= dtype :date)
          (let [d (parse-date-value v)]
            (case op
              ;; $eq on date: full-day match (same as scalar form)
              :$eq (let [end (.plusDays d 1)]
                     [:between sql-field (local-date-to-timestamp d) (local-date-to-timestamp end)])
              ;; $gte: from start of day
              :$gte [:>= sql-field (local-date-to-timestamp d)]
              ;; $lte: up to end of day (start of next day)
              :$lte [:<= sql-field (local-date-to-timestamp (.plusDays d 1))]))
          [(get op->sql op) sql-field parsed]))

      :else
      (throw (ex-info "Unsupported MQL predicate" {:field field-kw :pred pred :status 400})))))

(defn- mql-edn->where-clause
  "Convert MQL-style EDN filter to HoneySQL WHERE clause.
   Top-level can be: {:$and [...]} {:$or [...]} or a single field predicate map.
   table-aliases: {:rooms \"rs\"} — passed through to field-sql-expr."
  [edn fields-response* table-aliases]
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
    (and (map? edn) (contains? edn :$or))
    (let [raw-clauses (get edn :$or)
          clauses (if (sequential? raw-clauses) raw-clauses [raw-clauses])
          sql-clauses (map #(mql-edn->where-clause % fields-response* table-aliases) clauses)]
      (when (seq sql-clauses)
        (if (= (count sql-clauses) 1)
          (first sql-clauses)
          (vec (cons :or sql-clauses)))))

    ;; {:$and [pred1 pred2 ...]}
    (and (map? edn) (contains? edn :$and))
    (let [raw-clauses (get edn :$and)
          clauses (if (sequential? raw-clauses) raw-clauses [raw-clauses])
          sql-clauses (map #(mql-edn->where-clause % fields-response* table-aliases) clauses)]
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
      (mql-predicate->sql field-kw pred dtype table-aliases))

    ;; Multiple fields at top level => implicit $and
    (map? edn)
    (let [raw-clauses (map (fn [[f p]] (mql-edn->where-clause {f p} fields-response* table-aliases)) edn)
          clauses (filter some? raw-clauses)]
      (when (seq clauses)
        (if (= (count clauses) 1)
          (first clauses)
          (vec (cons :and clauses)))))

    :else
    (throw (ex-info INVALID_FILTER {:edn edn :status 400}))))

;; ------------------------------------------------------------
;; FILTER QUERY BUILDER
;; ------------------------------------------------------------

(defn create-filter-query-and-validate!
  "Apply MQL-style EDN filter to a HoneySQL query. table-aliases maps logical table names
   to the aliases used in the query, e.g. {:rooms \"rs\"} for [:rooms :rs] join."
  [query request filter-str table-aliases]
  (let [edn (parse-filter-edn filter-str)
        fields-response* (get-fields-response request)
        conditions (mql-edn->where-clause edn fields-response* table-aliases)]
    (if (seq conditions)
      (sql/where query conditions)
      query)))
