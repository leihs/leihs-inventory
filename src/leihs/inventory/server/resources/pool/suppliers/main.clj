(ns leihs.inventory.server.resources.pool.suppliers.main
  (:require
   [clojure.set]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.debug :refer [log-by-severity]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [leihs.inventory.server.utils.request-utils :refer [query-params]]
   [ring.middleware.accept]
   [ring.util.response :refer [response]]))

(def ERROR_GET_SUPPLIERS "Failed to get suppliers")

(def base-query
  (-> (sql/select :s.id :s.name :s.note)
      (sql/from [:suppliers :s])
      (sql/order-by :s.name)))

(defn index-resources
  ([request]
   (try
     (let [search-term (-> request query-params :search)
           base-query (-> base-query
                          (cond-> search-term
                            (sql/where [:ilike :s.name (str "%" search-term "%")])))]

       (response (create-pagination-response request base-query nil)))

     (catch Exception e
       (log-by-severity ERROR_GET_SUPPLIERS e)
       (exception-handler request ERROR_GET_SUPPLIERS e)))))
