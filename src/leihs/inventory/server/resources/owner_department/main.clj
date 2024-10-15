(ns leihs.inventory.server.resources.owner-department.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.core :refer [single-entity-get-request?]]
   [leihs.inventory.server.utils.pagination :refer [fetch-pagination-params]]
   [leihs.inventory.server.utils.pagination :refer [pagination-response create-pagination-response]]
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
       (create-pagination-response request base-query with-pagination?))
     (catch Exception e
       (error "Failed to get owner/department" e)
       (bad-request {:error "Failed to get owner/department" :details (.getMessage e)})))))

(defn get-owner-department-of-pool-auto-pagination-handler [request]
  (response (get-owner-department-of-pool request nil)))
