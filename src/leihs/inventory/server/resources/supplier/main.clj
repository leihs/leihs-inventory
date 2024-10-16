(ns leihs.inventory.server.resources.supplier.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.models.models-by-pool :refer [pagination-response valid-get-request?]]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [leihs.inventory.server.utils.pagination :refer [fetch-pagination-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]]))


(defn get-model-group-links-of-pool

  ([request]
   (get-model-group-links-of-pool request false))

  ([request with-pagination?]
   (try
     (let [tx (:tx request)
           pool_id (-> request path-params :pool_id)
           group_id (-> request path-params :supplier_id)
           {:keys [page size]} (fetch-pagination-params request)

           base-query (-> (sql/select :s.*)
                        (sql/from [:suppliers :s])
                        (cond-> group_id (sql/where [:= :s.id group_id]))
                        )]

       (cond
         (and (nil? with-pagination?) (valid-get-request? request)) (pagination-response request base-query)
         with-pagination? (pagination-response request base-query)
         :else (jdbc/query tx (-> base-query sql-format))))

     (catch Exception e
       (error "Failed to get supplier" e)
       (bad-request {:error "Failed to get supplier" :details (.getMessage e)})))))


(defn get-model-group-links-of-pool-handler [request]
  (response (get-model-group-links-of-pool request true)))

(defn get-model-group-links-of-pool-with-pagination-handler [request]
  (response (get-model-group-links-of-pool request true)))

(defn get-model-group-links-of-pool-auto-pagination-handler [request]
  (response (get-model-group-links-of-pool request nil)))

(defn get-model-group-links-of-pool-handler [request]
  (let [result (get-model-group-links-of-pool request)]
    (response result)))