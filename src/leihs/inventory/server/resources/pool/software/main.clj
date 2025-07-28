(ns leihs.inventory.server.resources.pool.software.main
  (:require
   [clojure.set]
   [clojure.string :as str]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]

   [leihs.inventory.server.resources.pool.software.types :as ty]

   [leihs.core.core :refer [presence]]
   [leihs.inventory.server.resources.pool.cast-helper :refer [remove-nil-entries-fnc
                                                              double-to-numeric-or-zero
                                                              double-to-numeric-or-nil]]
   [leihs.inventory.server.resources.pool.models.common :refer [fetch-thumbnails-for-ids
                                                                filter-map-by-spec]]

   [leihs.inventory.server.resources.pool.models.model.common-model-form :refer [extract-model-form-data
                                                                                 process-accessories
                                                                                 process-categories
                                                                                 process-compatibles
                                                                                 process-entitlements
                                                                                 process-properties]]


   [leihs.inventory.server.resources.pool.common :refer [ str-to-bool
                                                          process-attachments
                                                         ]]

   [leihs.inventory.server.resources.pool.models.helper :refer [
                                                                normalize-files
                                                                normalize-model-data

                                                                 ]]

   [leihs.inventory.server.resources.pool.models.queries :refer [base-inventory-query
                                                                 filter-by-type
                                                                 from-category
                                                                 with-items
                                                                 with-search
                                                                 without-items]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-to-response]]
   [leihs.inventory.server.utils.helper :refer [url-ends-with-uuid?]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response
                                                    fetch-pagination-params
                                                    fetch-pagination-params-raw]]
   [leihs.inventory.server.utils.request-utils :refer [path-params
                                                       query-params]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [debug error]])
  (:import
   (java.time LocalDateTime)))


(defn prepare-software-data
  [data]
  (let [normalize-data (normalize-model-data data)
        created-ts (LocalDateTime/now)]
    (assoc normalize-data
      :type "Software"
      :created_at created-ts
      :updated_at created-ts)))



(defn index-resources [request]
  (let [created-ts (LocalDateTime/now)
        tx (:tx request)
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        multipart (get-in request [:parameters :multipart])


        ;{:keys [product version manufacturer description]}
        ;(extract-model-form-data request)]

    multipart     (get-in request [:parameters :body])


        prepared-model-data (-> (prepare-software-data multipart)
                              (assoc :is_package (str-to-bool (:is_package multipart))))]

    (try
      (let [res (jdbc/execute-one! tx (-> (sql/insert-into :models)
                                        (sql/values [prepared-model-data])
                                        (sql/returning :*)
                                        sql-format))
            model-id (:id res)]

        (if res
          (response (filter-map-by-spec res ::ty/post-response))
          (bad-request {:error "Failed to create model"})))
      (catch Exception e
        (error "Failed to create model" (.getMessage e))
        (cond
          (str/includes? (.getMessage e) "unique_model_name_idx")
          (-> (response {:status "failure"
                         :message "Model already exists"
                         :detail {:product (:product prepared-model-data)}})
            (status 409))
          (str/includes? (.getMessage e) "insert or update on table \"models_compatibles\"")
          (-> (response {:status "failure"
                         :message "Modification of models_compatibles failed"
                         :detail {:product (:product prepared-model-data)}})
            (status 409))
          :else (bad-request {:error "Failed to create model" :details (.getMessage e)}))))))
