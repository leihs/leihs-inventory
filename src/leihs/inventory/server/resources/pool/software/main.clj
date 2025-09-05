(ns leihs.inventory.server.resources.pool.software.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.common :refer [fetch-attachments
                                                         str-to-bool]]
   [leihs.inventory.server.resources.pool.models.common :refer [filter-map-by-spec]]
   [leihs.inventory.server.resources.pool.models.helper :refer [normalize-model-data]]
   [leihs.inventory.server.resources.pool.software.types :as types]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.helper :refer [log-by-severity]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response]])
  (:import
   (java.time LocalDateTime)))

(def ERROR_CREATE_SOFTWARE "Failed to create software")

(defn prepare-software-data
  [data]
  (let [normalize-data (normalize-model-data data)
        created-ts (LocalDateTime/now)]
    (assoc normalize-data
           :type "Software"
           :created_at created-ts
           :updated_at created-ts)))

(defn post-resource [request]
  (let [tx (:tx request)
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        multipart (get-in request [:parameters :body])
        prepared-model-data (-> (prepare-software-data multipart)
                                (assoc :is_package (str-to-bool (:is_package multipart))))]

    (try
      (let [res (jdbc/execute-one! tx (-> (sql/insert-into :models)
                                          (sql/values [prepared-model-data])
                                          (sql/returning :*)
                                          sql-format))
            model-id (:id res)
            res (when res (let [attachments (fetch-attachments tx model-id pool-id)
                                result (assoc res :attachments attachments)] result))]

        (if res
          (response (filter-map-by-spec res ::types/post-response))
          (bad-request {:message "Failed to create software"})))
      (catch Exception e
        (log-by-severity ERROR_CREATE_SOFTWARE e)
        (exception-handler ERROR_CREATE_SOFTWARE e)))))
