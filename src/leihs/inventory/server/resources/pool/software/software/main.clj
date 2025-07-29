(ns leihs.inventory.server.resources.pool.software.software.main
  (:require
   [clojure.set]
   [clojure.string :as str]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.core :refer [presence]]

   [leihs.inventory.server.resources.pool.software.software.types :as ty]


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

   [leihs.inventory.server.resources.pool.models.queries :refer [base-inventory-query
                                                                 filter-by-type
                                                                 from-category
                                                                 with-items
                                                                 with-search
                                                                 without-items]]


   [leihs.inventory.server.resources.pool.models.model.main :refer [db-operation
                                                                    filter-keys
                                                                 ;db-operation-raw
                                                                 ;db-operation-raw-without-items
                                                                 ;db-operation-with-items
                                                                 ;db-operation-with-search
                                                                 ;db-operation-from-category
                                                                 ;db-operation-filter-by-type
                                                                 ;db-operation-base-inventory-query
                                                              ]]
   [leihs.inventory.server.resources.pool.common :refer [ str-to-bool
                                                         ;normalize-model-data
                                                         ;parse-json-array
                                                         ;file-to-base64
                                                         ;base-filename
                                                         parse-json-array
                                                         process-attachments

                                                         ]]
   [leihs.inventory.server.resources.pool.models.helper :refer [
                                                                normalize-files
                                                                normalize-model-data

                                                                ]]

   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-to-response]]
   [leihs.inventory.server.utils.helper :refer [url-ends-with-uuid?]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response
                                                    fetch-pagination-params
                                                    fetch-pagination-params-raw]]
   [leihs.inventory.server.utils.request-utils :refer [path-params
                                                       query-params]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status not-found]]
   [taoensso.timbre :refer [debug error]])
  (:import
   (java.time LocalDateTime)))


;(ns leihs.inventory.server.resources.models.form.software.model-by-pool-form-fetch
;  (:require
;   [clojure.data.json :as json]
;   [clojure.java.io :as io]
;   [clojure.string :as str]
;   [honey.sql :as sq :refer [format] :rename {format sql-format}]
;   [honey.sql.helpers :as sql]
;   [leihs.inventory.server.resources.models.queries :refer [attachments-query]]
;   [leihs.inventory.server.resources.utils.request :refer [path-params]]
;   [leihs.inventory.server.utils.converter :refer [to-uuid]]
;   [leihs.inventory.server.utils.pagination :refer [pagination-response]]
;   [next.jdbc :as jdbc]
;   [ring.util.response :refer [bad-request response]]
;   [taoensso.timbre :refer [error]])
;  (:import [java.time LocalDateTime]))

(defn select-entries [tx table columns where-clause]
  (jdbc/execute! tx
    (-> (apply sql/select columns)
      (sql/from table)
      (sql/where where-clause)
      sql-format)))

(defn fetch-attachments [tx model-id]
  (select-entries tx :attachments [:id :filename :content_type] [:= :model_id model-id]))

(defn get-resource [request]
  (let [tx (get-in request [:tx])
        model-id (to-uuid (get-in request [:path-params :model_id]))
        pool-id (to-uuid (get-in request [:path-params :pool_id]))]
    (try
      (let [model-query (-> (sql/select :m.id :m.product :m.manufacturer :m.version :m.type
                              :m.hand_over_note :m.description :m.internal_description
                              :m.technical_detail :m.is_package)
                          (sql/from [:models :m])
                          (sql/where [:and [:= :m.id model-id] [:= :m.type "Software"]])
                          sql-format)
            model-result (jdbc/execute-one! tx model-query)


            ;result (when model-result
            ;               (->> (fetch-attachments tx model-id)
            ;                 (assoc model-result :attachments )
            ;                 )
            ;
            ;               )

            ;attachments (fetch-attachments tx model-id)
            ;
            ;result (assoc model-result :attachments attachments)

            result (when model-result (let [
                                      attachments (fetch-attachments tx model-id)
                                      result (assoc model-result :attachments attachments)
                                      ]result))

            ;result (if model-result
            ;         [(assoc model-result :attachments attachments)]
            ;         [])
             ]
        (if result
          ;(response result)
          (response (filter-map-by-spec result ::ty/put-response))

          (not-found {:error "Failed to fetch software"})))
      (catch Exception e
        (error "Failed to fetch model" (.getMessage e))
        (bad-request {:error "Failed to fetch software" :details (.getMessage e)})))))


;
;(ns leihs.inventory.server.resources.models.form.software.model-by-pool-form-update
;  (:require
;   [cheshire.core :as cjson]
;   [clojure.data.codec.base64 :as b64]
;   [clojure.data.json :as json]
;   [clojure.java.io :as io]
;   [clojure.set :as set]
;   [clojure.string :as str]
;   [honey.sql :refer [format] :rename {format sql-format}]
;   [honey.sql.helpers :as sql]
;   [leihs.inventory.server.resources.models.form.common :refer [filter-keys db-operation]]
;   [leihs.inventory.server.resources.models.helper :refer [str-to-bool normalize-model-data normalize-files
;                                                           process-attachments parse-json-array]]
;   [leihs.inventory.server.utils.converter :refer [to-uuid]]
;   [next.jdbc :as jdbc]
;   [ring.util.response :refer [bad-request response]]
;   [taoensso.timbre :refer [error]])
;  (:import [java.time LocalDateTime]))

(defn prepare-software-data [data]
  (let [normalize-data (normalize-model-data data)
        created-ts (LocalDateTime/now)]
    (assoc normalize-data :updated_at created-ts :is_package (str-to-bool (:is_package normalize-data)))))

(defn process-deletions [tx ids table key]
  (doseq [id (set ids)]
    (jdbc/execute! tx (-> (sql/delete-from table)
                        (sql/where [:= key (to-uuid id)])
                        sql-format))))

(defn put-resource [request]
  (let [model-id (to-uuid (get-in request [:path-params :model_id]))
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        ;multipart (get-in request [:parameters :multipart])

        multipart     (get-in request [:parameters :body])


        tx (:tx request)
        prepared-model-data (prepare-software-data multipart)]
    (try
      (let [update-model-query (-> (sql/update [:models :m])
                                 (sql/set prepared-model-data)
                                 (sql/where [:and [:= :m.id model-id] [:= :m.type "Software"]])
                                 (sql/returning :*)
                                 sql-format)
            updated-model (jdbc/execute-one! tx update-model-query)
            ;attachments (normalize-files request :attachments)
            ;attachments-to-delete (parse-json-array request :attachments_to_delete)
            ]

        ;(process-attachments tx attachments model-id)
        ;(process-deletions tx attachments-to-delete :attachments :id)

        (if updated-model
          (response (filter-map-by-spec updated-model ::ty/put-response))
          ;(response [updated-model])
          (not-found {:error "Failed to update software"})))
      (catch Exception e
        (error "Failed to update model" (.getMessage e))
        (bad-request {:error "Failed to update software" :details (.getMessage e)})))))

(defn delete-resource [request]
  (let [pool-id (to-uuid (get-in request [:path-params :pool_id]))
        model-id (to-uuid (get-in request [:path-params :model_id]))
        tx (:tx request)
        where-clause-model [:and [:= :id model-id] [:= :type "Software"]]
        models (db-operation tx :select :models where-clause-model)
        _ (when-not (seq models)
            (throw (ex-info "Request to delete software blocked: software not found" {:status 404})))

        items (db-operation tx :select :items [:and [:= :model_id model-id]])
        attachments (db-operation tx :select :attachments [:= :model_id model-id])
        _ (when (seq items)
            (throw (ex-info "Request to delete software blocked: referenced item(s) exist" {:status 403})))

        deleted-model (jdbc/execute! tx (-> (sql/delete-from :models)
                                          (sql/where where-clause-model)
                                          (sql/returning :*)
                                          sql-format))
        remaining-attachments (db-operation tx :select :attachments [:= :model_id model-id])
        _ (when (seq remaining-attachments)
            (throw (ex-info "Request to delete software blocked: referenced attachments or images still exist" {:status 403})))

        result {:deleted_attachments (filter-keys attachments [:id :model_id :filename :size])
                :deleted_model (filter-keys deleted-model [:id :product :manufacturer])}]
    (if (= 1 (count deleted-model))
      (response result)
      (throw (ex-info "Request to delete software failed" {:status 403})))))
