(ns leihs.inventory.server.resources.pool.items.main
  (:require
   [clojure.set :as set]
   [clojure.string :as string]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.fields.main :as fields]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.helper :refer [log-by-severity]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [leihs.inventory.server.utils.request-utils :refer [path-params
                                                       query-params]]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response]]
   [taoensso.timbre :refer [debug error]]))

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
      (sql/group-by :m.product :i.model_id :i.inventory_code :i.inventory_pool_id :i.retired :m.is_package :i.id :i.parent_id)))

(defn index-resources
  ([request]
   (let [{:keys [pool_id item_id]} (path-params request)
         {:keys [search_term not_packaged packages retired result_type]} (query-params request)

         base-select (cond
                       (= result_type "Distinct") (sql/select-distinct-on [:m.product]
                                                                          :i.retired :i.parent_id :i.id
                                                                          :m.is_package
                                                                          :i.inventory_code
                                                                          :i.model_id
                                                                          :i.inventory_pool_id
                                                                          :m.product)
                       (= result_type "Min") (sql/select :i.retired :i.parent_id :i.id :i.inventory_code :i.model_id :m.is_package)
                       :else (sql/select :m.is_package :i.* [:b.name :building_name] [:r.name :room_name]))

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
                          (sql/where [:or [:ilike :i.inventory_code (str "%" search_term "%")] [:ilike :m.product (str "%" search_term "%")]
                                      [:ilike :m.manufacturer (str "%" search_term "%")]]))

                        (cond-> item_id (sql/where [:= :i.id item_id]))

                        (cond-> (and sort-by item_id) (sql/order-by item_id)))]

     (response (create-pagination-response request base-query nil)))))

(def ERROR_CREATE_ITEM "Failed to create item")

(def PROPERTIES_PREFIX "properties_")

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

(defn validate-field-permissions [tx role body-params]
  (let [permitted-fields (-> (fields/base-query "item" (keyword role))
                             sql-format
                             (->> (jdbc-query tx)))
        permitted-field-ids (->> permitted-fields
                                 (map (comp keyword :id))
                                 set)
        body-keys (set (keys body-params))
        unpermitted-fields (set/difference body-keys permitted-field-ids)]
    (when (seq unpermitted-fields)
      {:unpermitted-fields unpermitted-fields})))

(defn post-resource [request]
  (try
    (let [tx (:tx request)
          {:keys [role]} (:authenticated-entity request)
          body-params (get-in request [:parameters :body])
          validation-error (validate-field-permissions tx role body-params)]
      (if validation-error
        (bad-request {:error "Unpermitted fields" :details validation-error})
        (let [{:keys [item-data properties]} (split-item-data body-params)
              properties-json (or (not-empty properties) {})
              now (java.time.Instant/now)
              item-data-with-timestamps (assoc item-data
                                               :created_at now
                                               :updated_at now)
              item-data-with-properties (assoc item-data-with-timestamps
                                               :properties [:lift properties-json])
              sql-query (-> (sql/insert-into :items)
                            (sql/values [item-data-with-properties])
                            (sql/returning :*)
                            sql-format)
              result (jdbc/execute-one! tx sql-query)]
          (if result
            (response result)
            (bad-request {:error ERROR_CREATE_ITEM})))))
    (catch Exception e
      (log-by-severity ERROR_CREATE_ITEM e)
      (exception-handler request ERROR_CREATE_ITEM e))))
