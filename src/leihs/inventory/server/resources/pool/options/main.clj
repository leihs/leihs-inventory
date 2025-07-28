(ns leihs.inventory.server.resources.pool.options.main
  (:require
   [clojure.set]
   [clojure.string :as str]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.cast-helper :refer [double-to-numeric-or-nil]]
   [leihs.inventory.server.resources.pool.models.common :refer [filter-and-coerce-by-spec]]
   [leihs.inventory.server.resources.pool.options.types :as ty]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [debug error]])
  (:import
   (java.time LocalDateTime)))

(defn post-resource [request]
  (let [validation-result (atom [])
        created-ts (LocalDateTime/now)
        tx (:tx request)
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        option-id (to-uuid (get-in request [:path-params :option_id]))
        multipart (get-in request [:parameters :multipart])
        price (double-to-numeric-or-nil (:price multipart))
        multipart (assoc multipart :price price :inventory_pool_id pool-id)]

    (try
      (let [res (jdbc/execute-one! tx (-> (sql/insert-into :options)
                                        (sql/values [multipart])
                                        (sql/returning :*)
                                        sql-format))
            model-id (:id res)]

        (if res
          (response res)
          (bad-request {:error "Failed to create model"})))
      (catch Exception e
        (error "Failed to create model" (.getMessage e))
        (cond
          (str/includes? (.getMessage e) "unique_model_name_idx")
          (-> (response {:status "failure"
                         :message "Model already exists"
                         :detail {:product (:product multipart)}})
            (status 409))
          (str/includes? (.getMessage e) "insert or update on table \"models_compatibles\"")
          (-> (response {:status "failure"
                         :message "Modification of models_compatibles failed"
                         :detail {:product (:product multipart)}})
            (status 409))
          :else (bad-request {:error "Failed to create model" :details (.getMessage e)}))))))

(defn index-resources [request]
  (let [current-timestamp (LocalDateTime/now)
        tx (get-in request [:tx])
        pool-id (get-in request [:path-params :pool_id])]
    (try
      (let [base-query (->
                         (sql/select :o.*)
                         (sql/from [:options :o])
                         (sql/where [:= :o.inventory_pool_id [:cast pool-id :uuid]]))

            post-fnc (fn [models] (filter-and-coerce-by-spec models ::ty/response-option-object))]
        (response (create-pagination-response request base-query nil post-fnc)))
      (catch Exception e
        (error "Failed to fetch model" (.getMessage e))
        (bad-request {:error "Failed to fetch model" :details (.getMessage e)})))))
