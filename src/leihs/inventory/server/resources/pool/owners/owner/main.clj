(ns leihs.inventory.server.resources.pool.owners.owner.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [leihs.inventory.server.utils.request-utils :refer [path-params]]
   [next.jdbc :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]]))

(defn get-resource
  ([request]
   (get-resource request false))

  ([request with-pagination?]
   (try
     (let [tx (:tx request)
           pool_id (-> request path-params :pool_id)
           id (-> request path-params :id)
           base-query (-> (sql/select :i.id :i.name)
                          (sql/from [:inventory_pools :i])
                          (cond-> id (sql/where [:= :i.id id]))
                          (sql/order-by :i.name)
                          sql-format)]
       (response (jdbc/execute-one! tx base-query)))
     (catch Exception e
       (error "Failed to get owner/department" e)
       (bad-request {:error "Failed to get owner/department" :details (.getMessage e)})))))
