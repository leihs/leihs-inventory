(ns leihs.inventory.server.resources.pool.suppliers.supplier.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.core :refer [single-entity-get-request?]]
   [leihs.inventory.server.utils.pagination :refer [pagination-response fetch-pagination-params
                                                    fetch-pagination-params-raw]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]]))

(defn get-suppliers
  ([request]
   (get-suppliers request nil))

  ([request with-pagination?]
   (try
     (let [tx (:tx request)
           pool_id (-> request path-params :pool_id)
           group_id (-> request path-params :supplier_id)
           {:keys [page size]} (fetch-pagination-params-raw request)

           with-pagination? (if (and (nil? page) (nil? size)) false true)
           search-term (-> request query-params :search-term)

           base-query (-> (sql/select :s.id :s.name :s.note)
                          (sql/from [:suppliers :s])
                          (cond-> group_id (sql/where [:= :s.id group_id]))
                          (cond-> search-term (sql/where [:ilike :s.name (str "%" search-term "%")]))
                          (sql/order-by :s.name))]

       (cond
         (and (nil? with-pagination?) (single-entity-get-request? request)) (pagination-response request base-query)
         with-pagination? (pagination-response request base-query)
         :else (jdbc/query tx (-> base-query sql-format))))

     (catch Exception e
       (error "Failed to get supplier(s)" e)
       (bad-request {:error "Failed to get supplier(s)" :details (.getMessage e)})))))

(defn get-suppliers-auto-pagination-handler [request]
  (response (get-suppliers request nil)))

(defn get-suppliers-handler [request]
  (let [result (get-suppliers request)]
    (response result)))
