(ns leihs.inventory.server.resources.pool.options.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.cast-helper :refer [double-to-numeric-or-nil]]
   [leihs.inventory.server.resources.pool.models.common :refer [filter-and-coerce-by-spec
                                                                filter-map-by-spec]]
   [leihs.inventory.server.resources.pool.options.types :as ty]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.helper :refer [log-by-severity]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response]]))

(def FETCH_OPTIONS_ERROR "Failed to fetch options")
(def CREATE_OPTIONS_ERROR "Failed to create option")

(defn post-resource [request]
  (let [tx (:tx request)
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        multipart (get-in request [:parameters :body])
        price (double-to-numeric-or-nil (:price multipart))
        multipart (assoc multipart :price price :inventory_pool_id pool-id)]

    (try
      (let [res (jdbc/execute-one! tx (-> (sql/insert-into :options)
                                          (sql/values [multipart])
                                          (sql/returning :*)
                                          sql-format))]
        (if res
          (response (-> res
                        (filter-map-by-spec ::ty/response-option-object)))
          (bad-request {:message CREATE_OPTIONS_ERROR})))
      (catch Exception e
        (log-by-severity CREATE_OPTIONS_ERROR e)
        (exception-handler CREATE_OPTIONS_ERROR e)))))

(defn index-resources [request]
  (let [pool-id (get-in request [:path-params :pool_id])]
    (try
      (let [base-query (->
                        (sql/select :o.*)
                        (sql/from [:options :o])
                        (sql/where [:= :o.inventory_pool_id [:cast pool-id :uuid]]))

            post-fnc (fn [models] (filter-and-coerce-by-spec models ::ty/response-option-object))]
        (response (create-pagination-response request base-query nil post-fnc)))
      (catch Exception e
        (log-by-severity FETCH_OPTIONS_ERROR e)
        (exception-handler FETCH_OPTIONS_ERROR e)))))
