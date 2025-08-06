(ns leihs.inventory.server.resources.pool.options.option.main
  (:require
   [clojure.set]
   [clojure.string :as str]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.cast-helper :refer [double-to-numeric-or-nil]]
   [leihs.inventory.server.resources.pool.models.common :refer [filter-map-by-spec]]
   [leihs.inventory.server.resources.pool.options.types :as types]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.helper :refer [log-by-severity]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status]]))

(def UPDATE_OPTION_ERROR "Failed to update option")
(def DELETE_OPTION_ERROR "Failed to delete option")
(def FETCH_OPTION_ERROR "Failed to fetch option")

(defn get-resource [request]
  (let [tx (get-in request [:tx])
        option-id (to-uuid (get-in request [:path-params :option_id]))
        pool-id (to-uuid (get-in request [:path-params :pool_id]))]
    (try
      (let [model-query (->
                         (sql/select :o.*)
                         (sql/from [:options :o])
                         (sql/where [:= :o.id option-id])
                         (sql/where [:= :o.inventory_pool_id [:cast pool-id :uuid]])
                         sql-format)
            result (jdbc/execute-one! tx model-query)]

        (if result
          (response (-> result
                        (filter-map-by-spec ::types/response-option-object)))
          (bad-request {:error FETCH_OPTION_ERROR})))
      (catch Exception e
        (log-by-severity FETCH_OPTION_ERROR e)
        (bad-request {:error FETCH_OPTION_ERROR :details (.getMessage e)})))))

(defn put-resource [request]
  (let [option-id (to-uuid (get-in request [:path-params :option_id]))
        multipart (get-in request [:parameters :body])
        tx (:tx request)
        price (double-to-numeric-or-nil (:price multipart))
        multipart (assoc multipart :price price)]
    (try
      (let [update-model-query (-> (sql/update :options)
                                   (sql/set multipart)
                                   (sql/where [:= :id option-id])
                                   (sql/returning :*)
                                   sql-format)
            updated-model (jdbc/execute-one! tx update-model-query)]

        (if updated-model
          (response (-> updated-model
                        (filter-map-by-spec ::types/response-option-object)))
          (bad-request {:error UPDATE_OPTION_ERROR})))
      (catch Exception e
        (log-by-severity "Failed to update option" e)
        (cond
          (str/includes? (.getMessage e) "case_insensitive_inventory_code_for_options")
          (-> (response {:status "failure"
                         :message "Inventory code already exists"
                         :detail {:product (:product multipart)}})
              (status 409))
          :else (bad-request {:error UPDATE_OPTION_ERROR :details (.getMessage e)}))))))

(defn delete-resource [request]
  (let [option-id (to-uuid (get-in request [:path-params :option_id]))
        tx (:tx request)]
    (try
      (let [update-model-query (-> (sql/delete-from :options)
                                   (sql/where [:= :id option-id])
                                   (sql/returning :*)
                                   sql-format)
            deleted-model (jdbc/execute-one! tx update-model-query)]

        (if deleted-model
          (response (-> deleted-model
                        (filter-map-by-spec ::types/response-option-object)))
          (bad-request {:error "Failed to delete option"})))
      (catch Exception e
        (log-by-severity DELETE_OPTION_ERROR e)
        (bad-request {:error DELETE_OPTION_ERROR :details (.getMessage e)})))))
