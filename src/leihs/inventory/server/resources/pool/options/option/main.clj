(ns leihs.inventory.server.resources.pool.options.option.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.cast-helper :refer [double-to-numeric-or-nil]]
   [leihs.inventory.server.resources.pool.common :refer [is-option-deletable?]]
   [leihs.inventory.server.resources.pool.models.common :refer [filter-map-by-spec]]
   [leihs.inventory.server.resources.pool.options.option.types :as type]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.helper :refer [log-by-severity]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [not-found response]]))

(def DELETE_OPTION_ERROR "Failed to delete option")
(def FETCH_OPTION_ERROR "Failed to fetch option")
(def UPDATE_OPTION_ERROR "Failed to update option")

(defn get-resource [request]
  (try
    (let [tx (get-in request [:tx])
          option-id (to-uuid (get-in request [:path-params :option_id]))
          pool-id (to-uuid (get-in request [:path-params :pool_id]))
          query (-> (sql/select :o.*)
                    (sql/from [:options :o])
                    (sql/where [:= :o.id option-id])
                    (sql/where [:= :o.inventory_pool_id [:cast pool-id :uuid]])
                    sql-format)
          result (jdbc/execute-one! tx query)]

      (if result
        (response (-> result
                      (assoc :is_deletable (is-option-deletable? tx option-id))
                      (filter-map-by-spec ::type/get-response-option)))
        (not-found {:message FETCH_OPTION_ERROR})))
    (catch Exception e
      (log-by-severity FETCH_OPTION_ERROR e)
      (exception-handler FETCH_OPTION_ERROR e))))

(defn put-resource [request]
  (try
    (let [option-id (to-uuid (get-in request [:path-params :option_id]))
          multipart (get-in request [:parameters :body])
          tx (:tx request)
          price (double-to-numeric-or-nil (:price multipart))
          multipart (assoc multipart :price price)
          query (-> (sql/update :options)
                    (sql/set multipart)
                    (sql/where [:= :id option-id])
                    (sql/returning :*)
                    sql-format)
          updated-model (jdbc/execute-one! tx query)]

      (if updated-model
        (response (-> updated-model
                      (filter-map-by-spec ::type/get-response-option)))
        (not-found {:message UPDATE_OPTION_ERROR})))
    (catch Exception e
      (log-by-severity UPDATE_OPTION_ERROR e)
      (exception-handler UPDATE_OPTION_ERROR e))))

(defn delete-resource [request]
  (try
    (let [option-id (to-uuid (get-in request [:path-params :option_id]))
          tx (:tx request)
          query (-> (sql/delete-from :options)
                    (sql/where [:= :id option-id])
                    (sql/returning :*)
                    sql-format)
          deleted-option (jdbc/execute-one! tx query)]
      (if deleted-option
        (response (-> deleted-option
                      (filter-map-by-spec ::type/get-response-option)))
        (not-found {:message DELETE_OPTION_ERROR})))
    (catch Exception e
      (log-by-severity DELETE_OPTION_ERROR e)
      (exception-handler DELETE_OPTION_ERROR e))))
