(ns leihs.inventory.server.resources.pool.items.main
  (:require
   [clojure.set :as set]
   [clojure.string :as string]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.constants :refer [PROPERTIES_PREFIX]]
   [leihs.inventory.server.resources.pool.fields.main :as fields]
   [leihs.inventory.server.resources.pool.items.filter-handler :refer [add-filter-groups parse-json-param validate-filters]]
   [leihs.inventory.server.utils.authorize.main :refer [authorized-role-for-pool]]

   [leihs.inventory.server.utils.debug :refer [log-by-severity]]

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

(defn index-resources
  ([request]
   (let [{:keys [pool_id item_id]} (path-params request)
         {:keys [search_term
                 not_packaged
                 packages
                 retired
                 result_type]} (query-params request)

         base-select (cond
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

(defn patch-resource [{:keys [tx] :as request}]
  (try
    (if-let [validation-error (validate-field-permissions request)]
      (bad-request validation-error)
      (let [update-params (body-params request)
            {:keys [item-data properties]} (-> update-params (dissoc :id)
                                             split-item-data)
            properties-json (or (not-empty properties) {})
            item-data-with-properties (assoc item-data
                                        :properties [:lift properties-json])
            sql-query (-> (sql/update :items)
                        (sql/set item-data-with-properties)
                        (sql/where [:= :id (:id update-params)])
                        (sql/returning :*)
                        sql-format)
            result (jdbc/execute-one! tx sql-query)]
        (if result
          (response (flatten-properties result))
          (bad-request {:error ERROR_UPDATE_ITEM}))))
    (catch Exception e
      (log-by-severity ERROR_UPDATE_ITEM e)
      (exception-handler request ERROR_UPDATE_ITEM e))))


(def whitelist-keys
  [:inventory_code
   :price
   :supplier_id
   :retired
   ;:properties_maintenance_price
   :invoice_date])


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

(defn advanced-index-resources
  ([request]
   (try

     (let [{:keys [pool_id item_id]} (path-params request)
           {:keys [filters]} (query-params request)

           p (println ">o> abc.filter" filters)
           filters (parse-json-param filters)

           val-res (validate-filters filters whitelist-keys)
           p (println ">o> abc.val-res" val-res)
           ]

       (if (not-empty (:invalid val-res))
         (throw (ex-info "Invalid filter parameter!" {:status 400}))

         (let [
               p (println ">o> abc.filters" filters)
               base-query (-> (sql/select :i.*)
                            (sql/from [:items :i])


                            (cond-> (seq filters)
                              (add-filter-groups filters)
                              )

                            ;(sql/limit 10)
                            )
               p (println ">o> abc.base-query" (-> base-query sql-format))
               ]

           (response (create-pagination-response request base-query nil)))
         ))

     (catch Exception e
       (log-by-severity ERROR_ADVANCED_SEARCH e)
       (exception-handler request ERROR_ADVANCED_SEARCH e)))))


