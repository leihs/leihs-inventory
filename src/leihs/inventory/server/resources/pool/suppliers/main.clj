(ns leihs.inventory.server.resources.pool.suppliers.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.core :refer [single-entity-get-request?]]
   [leihs.inventory.server.utils.pagination :refer [pagination-response
                                                    create-pagination-response]]
   [leihs.inventory.server.utils.request-utils :refer [path-params]]
   [leihs.inventory.server.utils.request-utils :refer [path-params
                                                       query-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response]]
   [taoensso.timbre :refer [error]]))

(defn index-resources
  ([request]
   (try
     (let [tx (:tx request)
           pool_id (-> request path-params :pool_id)
           search-term (-> request query-params :search)
           base-query (-> (sql/select :s.id :s.name :s.note)
                          (sql/from [:suppliers :s])
                          (cond-> search-term (sql/where [:ilike :s.name (str "%" search-term "%")]))
                          (sql/order-by :s.name))]

       (response (create-pagination-response request base-query nil)))

     (catch Exception e
       (error "Failed to get supplier(s)" e)
       (bad-request {:error "Failed to get supplier(s)" :details (.getMessage e)})))))
