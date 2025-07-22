(ns leihs.inventory.server.resources.pool.suppliers.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.core :refer [single-entity-get-request?]]
   [leihs.inventory.server.utils.pagination :refer [
                                                    ;fetch-pagination-params-raw
                                                    pagination-response

                                                    create-pagination-response
                                                    ]]
   [leihs.inventory.server.utils.request-utils :refer [path-params]]
   [leihs.inventory.server.utils.request-utils :refer [path-params
                                                       query-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response]]
   [taoensso.timbre :refer [error]]))

(defn index-resources
  ([request]
  ; (get-suppliers request nil))
  ;
  ;([request with-pagination?]
   (try
     (let [tx (:tx request)
           pool_id (-> request path-params :pool_id)
           ;{:keys [page size]} (fetch-pagination-params-raw request)

           ;with-pagination? (if (and (nil? page) (nil? size)) false true)
           search-term (-> request query-params :search-term)

           base-query (-> (sql/select :s.id :s.name :s.note)
                          (sql/from [:suppliers :s])
                          (cond-> search-term (sql/where [:ilike :s.name (str "%" search-term "%")]))
                          (sql/order-by :s.name))]

       ;(cond
       ;  (and (nil? with-pagination?) (single-entity-get-request? request)) (pagination-response request base-query)
       ;  with-pagination? (pagination-response request base-query)
       ;  :else (jdbc/query tx (-> base-query sql-format)))


       (response (create-pagination-response request base-query nil)         )

       )

     (catch Exception e
       (error "Failed to get supplier(s)" e)
       (bad-request {:error "Failed to get supplier(s)" :details (.getMessage e)})))))

;(defn index-resources [request]
;  (response (get-suppliers request nil)))
