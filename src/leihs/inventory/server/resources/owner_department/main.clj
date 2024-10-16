(ns leihs.inventory.server.resources.owner-department.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.models.models-by-pool :refer [pagination-response valid-get-request?]]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.pagination :refer [fetch-pagination-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]]))

(defn get-owner-department-of-pool

  ([request]
   (get-owner-department-of-pool request false))

  ([request with-pagination?]
   (try
     (let [tx (:tx request)
           pool_id (-> request path-params :pool_id)
           id (-> request path-params :id)
           {:keys [page size]} (fetch-pagination-params request)

           base-query (-> (sql/select :i.id :i.name)
                          (sql/from [:inventory_pools :i])
                          (cond-> id (sql/where [:= :i.id id]))
                          (sql/order-by :i.name))]

       (cond
         (and (nil? with-pagination?) (valid-get-request? request)) (pagination-response request base-query)
         with-pagination? (pagination-response request base-query)
         :else (jdbc/query tx (-> base-query sql-format))))

     (catch Exception e
       (error "Failed to get supplier" e)
       (bad-request {:error "Failed to get supplier" :details (.getMessage e)})))))

(defn get-model-group-links-of-pool-handler [request]
  (response (get-owner-department-of-pool request true)))

(defn get-model-group-links-of-pool-with-pagination-handler [request]
  (response (get-owner-department-of-pool request true)))

(defn get-owner-department-of-pool-auto-pagination-handler [request]
  (response (get-owner-department-of-pool request nil)))

(defn get-model-group-links-of-pool-handler [request]
  (let [result (get-owner-department-of-pool request)]
    (response result)))