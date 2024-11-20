(ns leihs.inventory.server.resources.models.form.software.model-by-pool-form-update
  (:require
   [cheshire.core :as cjson]
   [clojure.data.codec.base64 :as b64]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.models.helper :refer [str-to-bool normalize-model-data normalize-files
                                                           process-attachments parse-json-array]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response]]
   [taoensso.timbre :refer [error]])
  (:import [java.time LocalDateTime]))

(defn prepare-software-data [data]
  (let [normalize-data (normalize-model-data data)
        created-ts (LocalDateTime/now)]
    (assoc normalize-data :updated_at created-ts :is_package (str-to-bool (:is_package normalize-data)))))

(defn process-deletions [tx ids table key]
  (doseq [id (set ids)]
    (jdbc/execute! tx (-> (sql/delete-from table)
                          (sql/where [:= key (to-uuid id)])
                          sql-format))))

(defn update-software-handler-by-pool-form [request]
  (let [model-id (to-uuid (get-in request [:path-params :model_id]))
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        multipart (get-in request [:parameters :multipart])
        tx (:tx request)
        prepared-model-data (prepare-software-data multipart)]
    (try
      (let [update-model-query (-> (sql/update [:models :m])
                                   (sql/set prepared-model-data)
                                   (sql/where [:and [:= :m.id model-id] [:= :m.type "Software"]])
                                   (sql/returning :*)
                                   sql-format)
            updated-model (jdbc/execute-one! tx update-model-query)
            attachments (normalize-files request :attachments)
            attachments-to-delete (parse-json-array request :attachments-to-delete)]
        (process-attachments tx attachments model-id)
        (process-deletions tx attachments-to-delete :attachments :id)
        (if updated-model
          (response [updated-model])
          (bad-request {:error "Failed to update model"})))
      (catch Exception e
        (error "Failed to update model" (.getMessage e))
        (bad-request {:error "Failed to update model" :details (.getMessage e)})))))
