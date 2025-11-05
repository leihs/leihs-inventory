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

      [clojure.string :as str]

      [cheshire.core :as json]
   ;   [clojure.string :as str]
   ;   [honey.sql :as hsql]
   ;   [honey.sql.helpers :as sql]
   ;   [next.jdbc :as jdbc]

   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [leihs.inventory.server.utils.request-utils :refer [body-params path-params
                                                       query-params]]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response]]
   [taoensso.timbre :as timbre :refer [debug spy]]))

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

(defn validate-field-permissions [request]
  (let [tx (:tx request)
        role (:role (:authenticated-entity request))
        body-params (body-params request)
        {pool-id :pool_id} (path-params request)
        item-id (:id body-params)
        permitted-fields (-> (fields/base-query "item" (keyword role) pool-id)
                           sql-format
                           (->> (jdbc-query tx)))
        permitted-field-ids (->> permitted-fields
                              (map (comp keyword :id))
                              set)
        body-keys (-> body-params (dissoc :id) keys set)
        unpermitted-fields (set/difference body-keys permitted-field-ids)
        {:keys [item-data]} (split-item-data body-params)
        owner-id (:owner_id item-data)]
    (cond
      (seq unpermitted-fields)
      {:error "Unpermitted fields" :unpermitted-fields unpermitted-fields}

      (or (and item-id (not= (authorized-role-for-pool request owner-id)
                         "inventory_manager")
            (not= owner-id pool-id))
        (and (not item-id) (not= owner-id pool-id)))
      {:error "Unpermitted owner_id"
       :provided owner-id
       :expected pool-id})))

(defn validate-field-permissions2 [request]
  (let [tx (:tx request)
        role (:role (:authenticated-entity request))
        body-params (body-params request)
        {pool-id :pool_id} (path-params request)


        item-id (:id body-params)
        item-data (:data body-params)

        ;; TODO: modify to handle ids(item-id) and item-data separately
        permitted-fields (-> (fields/base-query "item" (keyword role) pool-id)
                           sql-format
                           (->> (jdbc-query tx)))
        permitted-field-ids (->> permitted-fields
                              (map (comp keyword :id))
                              set)
        body-keys (-> body-params (dissoc :id) keys set)
        unpermitted-fields (set/difference body-keys permitted-field-ids)
        {:keys [item-data]} (split-item-data body-params)
        owner-id (:owner_id item-data)]
    (cond
      (seq unpermitted-fields)
      {:error "Unpermitted fields" :unpermitted-fields unpermitted-fields}

      (or (and item-id (not= (authorized-role-for-pool request owner-id)
                         "inventory_manager")
            (not= owner-id pool-id))
        (and (not item-id) (not= owner-id pool-id)))
      {:error "Unpermitted owner_id"
       :provided owner-id
       :expected pool-id})))


;(ns app.api.items
;  (:require
;   [clojure.set :as set]
;   [next.jdbc :as jdbc]
;   [honey.sql :as hsql]
;   [honey.sql.helpers :as sql]))

(defn validate-field-permissions2
  "Validates that all items in :ids can be updated by the user's role within the given pool.
   Returns nil if all checks pass, or
   {:error \"Permission check failed\" :error-items [...]} listing which items failed."
  [request]
  (let [tx (:tx request)
        role (-> request :authenticated-entity :role)
        body-params (body-params request)
        {pool-id :pool_id} (path-params request)

        ids (:ids body-params)
        data (:data body-params)

        ;; 🔍 Query which fields the role can modify
        permitted-fields (-> (fields/base-query "item" (keyword role) pool-id)
                           sql-format
                           (->> (jdbc/execute! tx)))
        permitted-field-ids (->> permitted-fields
                              (map (comp keyword :id))
                              set)

        ;; fields user wants to update
        data-keys (-> data keys set)
        unpermitted-fields (set/difference data-keys permitted-field-ids)]

    ;; ⚙️ Gather items for validation
    (if (seq ids)
      (let [items (jdbc/execute! tx
                    (sql-format
                      {:select [:id :owner_id]
                       :from [:items]
                       :where [:in :id ids]}))

            ;; 1️⃣ Owner check
            unauthorized-items
            (filter (fn [{:keys [id owner_id]}]
                      (and
                        (not= owner_id pool-id)
                        (not= (authorized-role-for-pool request owner_id)
                          "inventory_manager")))
              items)

            ;; 2️⃣ Combine errors
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
           :error-items error-items}))

      ;; 🧱 Fallback: no IDs provided
      {:error "Missing :ids in request body"})))



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

;(ns app.api.items
;  (:require
;   [cheshire.core :as json]
;   [clojure.string :as str]
;   [honey.sql :as hsql]
;   [honey.sql.helpers :as sql]
;   [next.jdbc :as jdbc]
;   [ring.util.http-response :refer [response bad-request]]))

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
    (if-let [validation-error (validate-field-permissions2 request)]
      (bad-request validation-error)

      (let [{:keys [ids data]} (body-params request)



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



(def whitelist-keys
  ["properties_ampere"
   "is_borrowable"
   "inventory_code"
   "building_id"
   "room_id"
   "is_incomplete"
   "properties_imei_number"
   "price"
   "invoice_date"
   "invoice_number"
   "last_check"
   "model_id"
   "note"
   "owner_id"
   "properties_p4u"
   "properties_reference"
   "is_inventory_relevant"
   "inventory_pool_id"
   "retired"
   "shelf"
   "serial_number"
   "status_note"
   "user_name"
   "properties_warranty_expiration"
   "properties_electrical_power"
   "supplier_id"
   "is_broken"])


;(defn filter-valid-keys
;  "Removes any keys from `data` maps that are not defined in the given `schema-map`."
;  [schema-map data]
;  (let [valid-keys (->> (keys schema-map)
;                     (map (fn [k]
;                            (if (instance? schema.core.OptionalKey k)
;                              (:k k) ; extract :k from (s/optional-key :foo)
;                              k)))
;                     set)]
;    (mapv #(select-keys % valid-keys) data)))

(defn advanced-index-resources
     [request]
     (try
       (let [{:keys [pool_id item_id]} (path-params request)
             {:keys [filters result_type]} (query-params request)
             parsed-filters (parse-json-param filters)
             validation-result (validate-filters parsed-filters whitelist-keys)]

         (if (not-empty (:invalid validation-result))
           (throw (ex-info "Invalid filter parameter!" {:status 400}))
           (let [

                 base-select (base-select result_type)
                 base-query (-> base-select
                                   (base-pool-query  pool_id)
                              (cond-> (seq parsed-filters)
                                (add-filter-groups parsed-filters)))


                 ;base-query (-> (sql/select :i.*)
                 ;               (sql/from [:items :i])
                 ;               (cond-> (seq parsed-filters)
                 ;                 (add-filter-groups parsed-filters)))

                 post-fnc (fn [items]
                            (println ">o> abc.count" (count items))
                            (println ">o> abc.seq?" (seq items))
                            (if (seq items)
                              items
                                ;(filter-map-by-schema items types/data-response)

                              items))


                 ]
             ;(response (create-pagination-response request base-query nil post-fnc)))))
             (response (create-pagination-response request base-query nil )))))
       (catch Exception e
         (log-by-severity ERROR_ADVANCED_SEARCH e)
         (exception-handler request ERROR_ADVANCED_SEARCH e))))


