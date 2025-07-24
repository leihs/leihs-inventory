(ns leihs.inventory.server.resources.pool.options.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
      [clojure.string :as str]
   [leihs.core.core :refer [presence]]
   [leihs.inventory.server.resources.pool.models.common :refer [fetch-thumbnails-for-ids
                                                                filter-map-by-spec]]
   [leihs.inventory.server.resources.pool.models.model.common-model-form :refer [extract-model-form-data
                                                                                 process-accessories
                                                                                 process-categories
                                                                                 process-compatibles
                                                                                 process-entitlements
                                                                                 process-properties]]


   [leihs.inventory.server.resources.pool.cast-helper :refer [remove-nil-entries-fnc
                                                                 double-to-numeric-or-zero
                                                              double-to-numeric-or-nil
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
;(ns leihs.inventory.server.resources.models.form.option.model-by-pool-form-create
;  (:require
;   [cheshire.core :as cjson]
;   [clojure.data.codec.base64 :as b64]
;   [clojure.data.json :as json]
;   [clojure.java.io :as io]
;   [clojure.set :as set]
;   [clojure.string :as str]
;   [honey.sql :refer [format] :rename {format sql-format}]
;   [honey.sql.helpers :as sql]
;   [leihs.inventory.server.resources.models.form.license.common :refer [double-to-numeric-or-nil create-validation-response]]
;   [leihs.inventory.server.resources.models.helper :refer [str-to-bool normalize-model-data parse-json-array normalize-files
;                                                           file-to-base64 base-filename process-attachments]]
;   [leihs.inventory.server.resources.models.queries :refer [accessories-query attachments-query base-pool-query
;                                                            entitlements-query item-query
;                                                            model-links-query properties-query]]
;   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
;   [leihs.inventory.server.utils.converter :refer [to-uuid]]
;   [leihs.inventory.server.utils.core :refer [single-entity-get-request?]]
;   [leihs.inventory.server.utils.helper :refer [convert-map-if-exist]]
;   [leihs.inventory.server.utils.pagination :refer [fetch-pagination-params pagination-response create-pagination-response]]
;   [next.jdbc :as jdbc]
;   [pantomime.extract :as extract]
;   [ring.util.response :refer [bad-request response status]]
;   [taoensso.timbre :refer [error]])
;  (:import [java.net URL JarURLConnection]
;   (java.time LocalDateTime)
;   [java.util UUID]
;   [java.util.jar JarFile]))

(defn create-option-handler-by-pool-form [request]
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
          ;(response (create-validation-response res @validation-result))
          ;(response (create-pagination-response request base-query nil)))
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


(defn fetch-option-handler-by-pool-form [request]
  (let [current-timestamp (LocalDateTime/now)
        tx (get-in request [:tx])
        ;option-id (to-uuid (get-in request [:path-params :option_id]))
        pool-id (to-uuid (get-in request [:path-params :pool_id]))]
    (try
      (let [model-query (->
                          (sql/select :o.*)
                          (sql/from [:options :o])
                          ;(sql/where [:= :o.id option-id])
                          sql-format)
            model-result (jdbc/execute-one! tx model-query)

            result (if model-result
                     [model-result]
                     [])]
        (if result
          (response result)
          (bad-request {:error "Failed to fetch model"})))
      (catch Exception e
        (error "Failed to fetch model" (.getMessage e))
        (bad-request {:error "Failed to fetch model" :details (.getMessage e)})))))
