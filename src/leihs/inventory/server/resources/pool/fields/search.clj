(ns leihs.inventory.server.resources.pool.fields.search
  (:require
   [clojure.set]
   [honey.sql :as sq]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.core :refer [single-entity-get-request?]]
   [leihs.inventory.server.utils.pagination :refer [fetch-pagination-params]]
   [leihs.inventory.server.utils.pagination :refer [pagination-response create-pagination-response]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]]))

(defn base-pool-query [query pool-id]
  (-> query
      (sql/from [:models :m])
      (cond->
       pool-id (sql/join [:model_links :ml] [:= :m.id :ml.model_id])
       pool-id (sql/join [:model_groups :mg] [:= :mg.id :ml.model_group_id])
       pool-id (sql/join [:inventory_pools_model_groups :ipmg] [:= :mg.id :ipmg.model_group_id])
       pool-id (sql/join [:inventory_pools :ip] [:= :ip.id :ipmg.inventory_pool_id])
       pool-id (sql/where [:= :ip.id [:cast pool-id :uuid]]))))

(defn get-form-fields
  ([request]
   (get-form-fields request false))
  ([request with-pagination?]
   (try
     (let [tx (:tx request)
           pool_id (-> request path-params :pool_id)
           group_id (-> request path-params :field_id)
           {:keys [type]} (-> request query-params)
           {:keys [page size]} (fetch-pagination-params request)
           user-id (:id (:authenticated-entity request))
           base-query (-> (sql/select :m.*)

                          ((fn [query] (base-pool-query query pool_id)))

                          (cond-> type (sql/where [:= :m.type type]))
                          (sql/where [:= :ipmg.inventory_pool_id pool_id]))
           cus-fnc (fn [result] (map #(hash-map "model" %) result))]
       (create-pagination-response request base-query with-pagination?))

     (catch Exception e
       (error "Failed to get supplier(s)" e)
       (bad-request {:error "Failed to get supplier(s)" :details (.getMessage e)})))))

(defn get-search-auto-pagination-handler [request]
  (response (get-form-fields request nil)))

(defn get-search-with-pagination-handler [request]
  (response (get-form-fields request true)))

(defn get-search-handler [request]
  (let [result (get-form-fields request)]
    (response result)))
