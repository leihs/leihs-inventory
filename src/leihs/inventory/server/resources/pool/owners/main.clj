(ns leihs.inventory.server.resources.pool.owners.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.request-utils :refer [path-params]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request header response]]
   [taoensso.timbre :refer [error]]))

;(ns leihs.inventory.server.resources.owner-department.main
;  (:require
;   [clojure.set]
;   [honey.sql :refer [format] :rename {format sql-format}]
;   [honey.sql.helpers :as sql]
;   [leihs.inventory.server.resources.utils.request :refer [path-params]]
;   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
;   [leihs.inventory.server.utils.core :refer [single-entity-get-request?]]
;   [leihs.inventory.server.utils.pagination :refer [fetch-pagination-params]]
;   [leihs.inventory.server.utils.pagination :refer [pagination-response create-pagination-response]]
;   [next.jdbc.sql :as jdbc]
;   [ring.middleware.accept]
;   [ring.util.response :refer [bad-request response status]]
;   [taoensso.timbre :refer [error]]))

(defn index-resources
  ([request]
   (try
     (let [tx (:tx request)
           pool_id (-> request path-params :pool_id)
           id (-> request path-params :id)
           base-query (-> (sql/select :i.id :i.name)
                        (sql/from [:inventory_pools :i])
                        (cond-> id (sql/where [:= :i.id id]))
                        (sql/order-by :i.name))]
       (response (create-pagination-response request base-query nil)))
     (catch Exception e
       (error "Failed to get owner/department" e)
       (bad-request {:error "Failed to get owner/department" :details (.getMessage e)})))))
