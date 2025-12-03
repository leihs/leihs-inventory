(ns leihs.inventory.server.resources.pool.items.filter-handler
  (:require
   [cheshire.core :as json]
   [clojure.edn :as edn]
   [clojure.pprint :as pp]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.fields.main :as fields]
   [leihs.inventory.server.utils.debug :refer [log-by-severity]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.request-utils :refer [path-params query-params]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response header]]
   [taoensso.timbre :refer [error]])
  (:import
   [java.sql Timestamp]
   [java.time LocalDate ZoneId]
   [java.util UUID]))

;; ------------------------------------------------------------
;; CONSTANTS
;; ------------------------------------------------------------

(def ERROR_GET "Query error in list-pool-items (advanced filter)")
(def INVALID_FILTER "Invalid filter field")

(def ^:private operator-map
  {"~" :=
   "~~" :ilike
   ">" :>
   "<" :<
   ">=" :>=
   "<=" :<=
   "!" :not=
   "isnull" :isnull
   "isnotnull" :isnotnull})

(def ^:private uuid-fields
  #{:id :inventory_pool_id :owner_id :supplier_id :model_id :room_id :building_id})

(def ^:private numeric-fields
  #{:price})

(def ^:private boolean-fields
  #{:is_inventory_relevant :retired :is_broken :is_borrowable :is_incomplete})

(def ^:private date-fields
  #{:last_check :invoice_date})

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

(defn infer-data-type [field]
  (cond
    (contains? uuid-fields field) :uuid
    (contains? numeric-fields field) :numeric
    (contains? boolean-fields field) :boolean
    (contains? date-fields field) :date
    :else :text))

;; ------------------------------------------------------------
;; FILTER PARSING
;; ------------------------------------------------------------

(defn extract-field-and-op [filter-str]
  (let [pattern #"^([^>=<~!]+)(>=|<=|=|~|>|<|!)(.+)$"
        [_ field op value] (re-matches pattern filter-str)]
    (if (and field op value)
      {:field (str/trim field)
       :op (str/trim op)
       :value (str/trim value)}
      (throw (ex-info "Invalid filter format" {:filter filter-str :status 400})))))

(defn validate-field [field fields-response*]
  (let [field-kw (keyword field)
        allowed (set (map :id fields-response*))]
    (when-not (contains? allowed field-kw)
      (throw (ex-info INVALID_FILTER {:field field :status 400})))))

(defn parse-filter-single [filter-str fields-response*]
  (let [unary-pattern #"^([^\s>=<~!]+)\s+(isnull|isnotnull)$"
        binary-pattern #"^([^>=<~!]+)(>=|<=|~~|~|>|<|!)(.+)$"
        um (re-matches unary-pattern filter-str)
        bm (re-matches binary-pattern filter-str)
        {:keys [field op value]}
        (cond
          um (let [[_ f o] um] {:field f :op o})
          bm (let [[_ f o v] bm] {:field f :op o :value v})
          :else (extract-field-and-op filter-str))]

    (validate-field field fields-response*)

    ;; Handle "~" vs "~~"
    (let [op* (if (and (= op "~") (str/starts-with? value "~")) "~~" op)
          value* (if (and (= op "~") (str/starts-with? value "~"))
                   (subs value 1)
                   value)
          op-key (operator-map op*)]
      (when-not op-key
        (throw (ex-info "Unsupported operator" {:operator op* :status 400})))

      (let [field-kw (keyword field)
            field-info (get-field-info field-kw fields-response*)
            data-type (or (:data_type field-info)
                          (infer-data-type field-kw))]

        {:field field-kw
         :op op-key
         :value value*
         :data_type data-type}))))

(defn parse-filter [filter-str fields-response*]
  (let [and-parts (str/split filter-str #"\|\|")]
    (map (fn [and-part]
           (map #(parse-filter-single % fields-response*)
                (str/split and-part #"\|")))
         and-parts)))

(defn get-fields-response [request]
  (-> (fields/index-resources
       (assoc-in request [:parameters :query :target_type] "item"))
      :body
      :fields
      extract-id-type))

(defn normalize-pipes [s]
  (-> s
      (str/replace #" *\|\| *" "||")
      (str/replace #" *\| *" "|")))

;; ------------------------------------------------------------
;; DATE HANDLING
;; ------------------------------------------------------------

(defn parse-date-value [value op]
  (cond
    ;; Range format: =2020-01-01;2020-01-10
    (and (= op :=) (str/includes? value ";"))
    (let [[s e] (str/split value #";")
          s (LocalDate/parse s)
          e (.plusDays (LocalDate/parse e) 1)]
      {:type :range :start s :end e})

    ;; Single-day exact match
    (= op :=)
    (let [d (LocalDate/parse value)]
      {:type :range :start d :end (.plusDays d 1)})

    ;; Comparisons
    :else
    (LocalDate/parse value)))

(defn local-date-to-timestamp [local-date]
  (-> local-date
      (.atStartOfDay (ZoneId/of "UTC"))
      .toInstant
      Timestamp/from))

;; ------------------------------------------------------------
;; QUERY BUILDING
;; ------------------------------------------------------------

(defn parse-value-by-type [value data-type op]
  (try
    (case data-type
      :uuid (UUID/fromString value)
      :integer (Integer/parseInt value)
      :bigint (Long/parseLong value)
      :decimal (bigdec value)
      :numeric (bigdec value)
      :boolean (Boolean/parseBoolean value)
      :date (parse-date-value value op)
      value)
    (catch Exception _ value)))

(defn build-where-clause-single [{:keys [field op value data_type]} pool-id]
  (if (str/starts-with? (name field) "properties_")
  ;; JSON properties field    ;; JSON properties field
    (let [key (subs (name field) (count "properties_"))]
      (case op
        := [:= [:raw (format "properties->>'%s'" key)] value]
        :ilike [:ilike [:raw (format "properties->>'%s'" key)] (str "%" value "%")]
        :> [:> [:raw (format "properties->>'%s'" key)] value]
        :< [:< [:raw (format "properties->>'%s'" key)] value]
        :<= [:<= [:raw (format "properties->>'%s'" key)] value]
        :>= [:>= [:raw (format "properties->>'%s'" key)] value]
        :not= [:not= [:raw (format "properties->>'%s'" key)] value]
        :isnull [:is [:raw (format "properties->>'%s'" key)] nil]
        :isnotnull [:is-not [:raw (format "properties->>'%s'" key)] nil]
        (throw (ex-info "Unsupported operator" {:operator op :status 400}))))

    ;; Normal DB field
    (let [parsed (parse-value-by-type value data_type op)]
      (if (= data_type :date)
        ;; Date handling
        (if (and (map? parsed) (= (:type parsed) :range))
          (let [{:keys [start end]} parsed]
            [:between field (local-date-to-timestamp start)
             (local-date-to-timestamp end)])
          (case op
            :> [:> field (local-date-to-timestamp parsed)]
            :< [:< field (local-date-to-timestamp parsed)]
            :>= [:>= field (local-date-to-timestamp parsed)]
            :<= [:<= field (local-date-to-timestamp parsed)]
            :not= [:not= field (local-date-to-timestamp parsed)]
            (throw (ex-info "Unsupported date operator" {:operator op :status 400}))))
        ;; Non-date field
        (case op
          := [:= field parsed]
          :ilike [:ilike field (str "%" value "%")]
          :> [:> field parsed]
          :< [:< field parsed]
          :>= [:>= field parsed]
          :<= [:<= field parsed]
          :not= [:not= field parsed]
          :isnull [:is field nil]
          :isnotnull [:is-not field nil]
          (throw (ex-info "Unsupported operator" {:operator op :status 400})))))))

(defn build-where-clause [parsed-filters pool-id]
  (let [groups
        (map (fn [or-group]
               (let [ands (map #(build-where-clause-single % pool-id) or-group)]
                 (if (> (count ands) 1)
                   (vec (cons :and ands))
                   (first ands))))
             parsed-filters)]
    (if (> (count groups) 1)
      (vec (cons :or groups))
      (first groups))))

(defn build-query [parsed-filters pool-id]
  (let [base [:= :inventory_pool_id pool-id]]
    (if (seq parsed-filters)
      {:select [:*]
       :from [:items]
       :where [:and base (build-where-clause parsed-filters pool-id)]
       :limit 100}
      {:select [:*]
       :from [:items]
       :where base
       :limit 100})))

;; ------------------------------------------------------------
;; THIS FUNCTION WAS "UNUSED" — NOW RESTORED
;; ------------------------------------------------------------

(defn create-filter-query-and-validate! [query request pool-id filter-str]
  (let [filter-str (normalize-pipes filter-str)
        fields-response* (extract-id-type (get-fields-response request))
        parsed (parse-filter filter-str fields-response*)
        conditions (build-where-clause parsed pool-id)]
    (sql/where query conditions)))

;; ------------------------------------------------------------
;; MAIN HANDLER
;; ------------------------------------------------------------

(defn list-pool-items [request]
  (try
    (let [tx (:tx request)
          pool-id (UUID/fromString (get-in request [:path-params :pool_id]))
          filter-str (some-> (get-in request [:parameters :query :filter]) normalize-pipes)
          fields* (get-fields-response request)
          parsed (when filter-str (parse-filter filter-str fields*))
          query (build-query parsed pool-id)
          sql (sql-format query)
          items (jdbc/execute! tx sql)]

      (-> (response {:sub_query sql
                     :items items})
          (header "X-Count" (count items))))

    (catch Exception e
      (error ERROR_GET (.getMessage e))
      (exception-handler request (.getMessage e) e))))
