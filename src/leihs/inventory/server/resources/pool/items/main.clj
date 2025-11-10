(ns leihs.inventory.server.resources.pool.items.main
  (:require
   [clojure.set :as set]
   [clojure.string :as string]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]

   [leihs.inventory.server.resources.pool.items.types :as types]
   [leihs.inventory.server.resources.pool.models.common :refer [filter-map-by-schema]]
   [leihs.inventory.server.constants :refer [PROPERTIES_PREFIX]]
   [leihs.inventory.server.resources.pool.fields.main :as fields]
   [leihs.inventory.server.resources.pool.items.filter-handler :refer [add-filter-groups parse-json-param validate-filters]]
   [leihs.inventory.server.utils.authorize.main :refer [authorized-role-for-pool]]
   [leihs.inventory.server.utils.debug :refer [log-by-severity]]
   [leihs.inventory.server.resources.pool.fields.main :refer [fetch-properties-fields]]
      [clojure.string :as str]
      [cheshire.core :as json]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [leihs.inventory.server.utils.request-utils :refer [body-params path-params
                                                       query-params]]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response]]
   [taoensso.timbre :as timbre :refer [debug spy]])
  (:import [java.time Instant])  )

(defn base-pool-query [query pool-id]
  (-> query
    (sql/from [:items :i])
    (sql/join [:rooms :r] [:= :r.id :i.room_id])
    (sql/join [:models :m] [:= :m.id :i.model_id])
    (sql/join [:buildings :b] [:= :b.id :r.building_id])
    (cond->
      pool-id (sql/join [:inventory_pools :ip] [:= :ip.id :i.inventory_pool_id])
      pool-id (sql/where [:= :ip.id [:cast pool-id :uuid]]))))

(defn base-pool-query-distinct [query pool-id]
  (-> query
    (sql/from [:items :i])
    (sql/join [:models :m] [:= :m.id :i.model_id])
    (cond-> pool-id (sql/where [:= :i.inventory_pool_id [:cast pool-id :uuid]]))
    (sql/group-by :m.product
      :i.model_id
      :i.inventory_code
      :i.inventory_pool_id
      :i.retired
      :m.is_package
      :i.id
      :i.parent_id)))


(defn base-select [result_type] (cond
              (= result_type "Distinct")
              (sql/select-distinct-on [:m.product]
                :i.retired :i.parent_id :i.id
                :m.is_package
                :i.inventory_code
                :i.model_id
                :i.inventory_pool_id
                :m.product)
              (= result_type "Min")
              (sql/select :i.retired
                :i.parent_id
                :i.id
                :i.inventory_code
                :i.model_id
                :m.is_package
                :m.product
                [:b.name :building_name]
                [:r.name :room_name]
                )
              :else (sql/select :m.is_package
                      :i.*
                      [:b.name :building_name]
                      [:r.name :room_name]))

)
(defn index-resources
  ([request]
   (let [{:keys [pool_id item_id]} (path-params request)
         {:keys [search_term
                 not_packaged
                 packages
                 retired
                 result_type]} (query-params request)

         base-select (base-select result_type)

         base-query (-> base-select
                      ((fn [query]
                         (if (= result_type "Distinct")
                           (base-pool-query-distinct query pool_id)
                           (base-pool-query query pool_id))))

                      (cond-> item_id (sql/where [:= :i.id item_id]))

                      (cond-> (= true retired) (sql/where [:is-not :i.retired nil]))
                      (cond-> (= false retired) (sql/where [:is :i.retired nil]))

                      (cond-> (= true packages) (sql/where [:= :m.is_package true]))
                      (cond-> (= false packages) (sql/where [:= :m.is_package false]))

                      (cond-> (= true not_packaged) (sql/where [:is :i.parent_id nil]))
                      (cond-> (= false not_packaged) (sql/where [:is-not :i.parent_id nil]))

                      (cond-> (seq search_term)
                        (sql/where [:or
                                    [:ilike :i.inventory_code (str "%" search_term "%")]
                                    [:ilike :m.product (str "%" search_term "%")]
                                    [:ilike :m.manufacturer (str "%" search_term "%")]]))

                      (cond-> item_id (sql/where [:= :i.id item_id]))

                      (cond-> (and sort-by item_id) (sql/order-by item_id)))]

     (response (create-pagination-response request base-query nil)))))

(def ERROR_CREATE_ITEM "Failed to create item")
(def ERROR_ADVANCED_SEARCH "Failed to search item")

(defn split-item-data [body-params]
  (let [field-keys (keys body-params)
        properties-keys (filter #(string/starts-with? (name %) PROPERTIES_PREFIX)
                          field-keys)
        item-keys (remove #(string/starts-with? (name %) PROPERTIES_PREFIX)
                    field-keys)
        properties (->> properties-keys
                     (map (fn [k]
                            [(-> k
                               name
                               (string/replace
                                 (re-pattern (str "^" PROPERTIES_PREFIX)) "")
                               keyword)
                             (get body-params k)]))
                     (into {}))
        item-data (select-keys body-params item-keys)]
    {:item-data item-data
     :properties properties}))

(defn validate-field-permissions
  "Validates that one or more items can be updated by the user's role within the given pool.
   Handles both single-item (:id) and multi-item (:ids) requests.
   Returns nil if all checks pass, or an error map with details."
  [request]
  (let [tx (:tx request)
        role (-> request :authenticated-entity :role)
        body-params (body-params request)
        {pool-id :pool_id} (path-params request)
        ;; Support both single and multiple IDs
        ids (cond
              (:ids body-params) (:ids body-params)
              (:id body-params)  [(:id body-params)]
              :else nil)

        ;; Data structure depends on single vs multi payload
        data (or (:data body-params)
               (dissoc body-params :id))

        ;; Fetch permitted fields for the role & pool
        permitted-fields (-> (fields/base-query "item" (keyword role) pool-id)
                           sql-format
                           (->> (jdbc/execute! tx)))
        permitted-field-ids (->> permitted-fields
                              (map (comp keyword :id))
                              set)

        ;; Check unpermitted fields
        data-keys (-> data keys set)
        unpermitted-fields (set/difference data-keys permitted-field-ids)]

    (cond
      (empty? ids)
      {:error "Missing :id or :ids in request body"}

      (seq unpermitted-fields)
      {:error "Unpermitted fields"
       :unpermitted-fields unpermitted-fields}

      :else
      (let [items (jdbc/execute! tx
                    (sql-format
                      {:select [:id :owner_id]
                       :from [:items]
                       :where [:in :id ids]}))

            unauthorized-items
            (filter (fn [{:keys [id owner_id]}]
                      (and
                        (not= owner_id pool-id)
                        (not= (authorized-role-for-pool request owner_id)
                          "inventory_manager")))
              items)

            ;; Build detailed error reports
            error-items
            (concat
              (when (seq unpermitted-fields)
                (map (fn [{:keys [id]}]
                       {:id id
                        :reason :unpermitted-fields
                        :fields unpermitted-fields})
                  items))
              (map (fn [{:keys [id owner_id]}]
                     {:id id
                      :reason :unpermitted-owner
                      :provided owner_id
                      :expected pool-id})
                unauthorized-items))]

        (when (seq error-items)
          {:error "Permission check failed"
           :error-items error-items})))))



(defn flatten-properties [item]
  (let [properties (:properties item)
        properties-with-prefix
        (reduce (fn [acc [k v]]
                  (assoc acc (keyword (str PROPERTIES_PREFIX (name k))) v))
          {}
          properties)
        item-without-properties (dissoc item :properties)]
    (merge item-without-properties properties-with-prefix)))

(defn post-resource [request]
  (try
    (let [tx (:tx request)
          body-params (body-params request)
          validation-error (validate-field-permissions request)]
      (if validation-error
        (bad-request validation-error)
        (let [{:keys [item-data properties]} (split-item-data body-params)
              properties-json (or (not-empty properties) {})
              item-data-with-properties (assoc item-data
                                          :properties [:lift properties-json])
              sql-query (-> (sql/insert-into :items)
                          (sql/values [item-data-with-properties])
                          (sql/returning :*)
                          sql-format)
              result (jdbc/execute-one! tx sql-query)]
          (if result
            (response (flatten-properties result))
            (bad-request {:error ERROR_CREATE_ITEM})))))
    (catch Exception e
      (log-by-severity ERROR_CREATE_ITEM e)
      (exception-handler request ERROR_CREATE_ITEM e))))

(def ERROR_UPDATE_ITEM "Failed to update item")


;; TODO: refactor to patch update items
(defn log-map
  "Logs each key, value, and value type from the given map."
  [m]
  (doseq [[k v] m]
    (println "key:" k
      "| value:" v
      "| type:" (type v))))


(defn patch-resource
  [{:keys [tx] :as request}]
  (try
    ;; Optional permission validation
    (if-let [validation-error (validate-field-permissions request)]
      (bad-request validation-error)

      (let [     p (println ">o> abc.patch-resource1" patch-resource)
            {:keys [ids data]} (body-params request)
            p (println ">o> abc.patch-resource.data" data)
            ids (set ids)

            ;; Split :properties_* fields into JSON props vs normal DB fields
            [item-fields prop-fields]
            (reduce-kv
              (fn [[norm props] k v]
                (let [kname (name k)]
                  (if (str/starts-with? kname "properties_")
                    [norm (assoc props (subs kname (count "properties_")) v)]
                    [(assoc norm k v) props])))
              [{} {}]
              data)

            ;; Merge JSONB fields into existing properties column
            set-map
            (cond-> item-fields
              (seq prop-fields)
              (assoc :properties
                [:||
                 :properties
                 [:cast (json/generate-string prop-fields) :jsonb]]))


            _ (log-map set-map)

            ;; Build HoneySQL 2 query using helper functions
            query (-> (sql/update :items)
                      (sql/set set-map)
                      (sql/where [:in :id ids])
                      (sql/returning :*)

                      sql-format
                      )

            ;; Convert to SQL string + params
            ;[query & params] (sql-format sql-map)
            ;[query & params] (sql-format sql-map)
            ;results (jdbc/execute! tx (into [query] params))]


            p (println ">o> abc.query" query)
            results (jdbc/execute! tx query)

            p (println ">o> abc.results" results)

            ]

        (if (seq results)
          (response (map flatten-properties results))
          (bad-request {:error ERROR_UPDATE_ITEM}))))

    (catch Exception e
      (println ">o> abc.e" e)
      (log-by-severity ERROR_UPDATE_ITEM e)
      (exception-handler request ERROR_UPDATE_ITEM e))))


(def property-keys
  #{"electrical_power" "imei_number" "ampere" "warranty_expiration" "reference"})

(defn ^:private props-key
  "If k's name is in property-keys, prefix with properties_. Preserve key type.
   Avoid double-prefixing."
  [k]
  (let [kname (name k)]
    (if (and (property-keys kname)
          (not (str/starts-with? kname "properties_")))
      (cond
        (keyword? k) (keyword (str "properties_" kname))
        (string?  k) (str "properties_" kname)
        :else k)
      k)))

(defn ^:private rename-keys-rec
  "Recursively rename keys in any nested map/vector/seq using props-key."
  [x]
  (cond
    (map? x)
    ;; preserve map type (hash-map, array-map, ordered map)
    (into (empty x)
      (map (fn [[k v]]
             [(props-key k) (rename-keys-rec v)]))
      x)

    (vector? x) (mapv rename-keys-rec x)
    (sequential? x) (doall (map rename-keys-rec x))
    :else x))

(defn prepare-filters [filters]
  (cond-> filters
    (contains? filters :retired)
    (assoc :retired (case (:retired filters)
                      true  (Instant/now)
                      false nil
                      (:retired filters)))))

(defn extract-ids
  "Extracts :id values from a vector of maps.
   Returns:
   {:keys       [ids-without-prefix]
    :properties [original-prefixed-ids]
    :raw-keys   [all-original-ids]}"
  [fields prefix-to-remove]
  (let [ids (keep :id fields)
        prefixed? #(str/starts-with? (name %) prefix-to-remove)
        properties (filter prefixed? ids)
        keys (mapv (fn [id]
                     (let [id-str (name id)]
                       (-> id-str
                         (str/replace
                           (re-pattern (str "^" (java.util.regex.Pattern/quote prefix-to-remove)))
                           "")
                         keyword)))
               ids)]
    {:filter-keys keys
     :properties (vec properties)
     :raw-filter-keys (vec ids)}))
