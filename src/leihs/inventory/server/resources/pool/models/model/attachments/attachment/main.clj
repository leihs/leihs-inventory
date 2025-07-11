(ns leihs.inventory.server.resources.pool.models.model.attachments.attachment.main
  (:require
   [clojure.data.codec.base64 :as b64]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response]]
   [taoensso.timbre :refer [error]])
  (:import
   [java.util Base64]))

(defn get-resource [request]
  (try
    (let [tx (:tx request)
          id (-> request path-params :attachments_id)
          model-id (-> request path-params :model_id)

          accept-header (get-in request [:headers "accept"])
          content-disposition (or (-> request :parameters :query :content_disposition) "inline")
          type (or (-> request :parameters :query :type) "new")
          query (-> (sql/select :a.*)
                    (sql/from [:attachments :a])
                  (cond-> id (sql/where [:= :a.model_id model-id]))

                  (cond-> id (sql/where [:= :a.id id]))
                    sql-format)
          attachment (jdbc/execute-one! tx query)
          ;attachment (first result)
          base64-string (:content attachment)
          file-name (:filename attachment)
          content-type (:content_type attachment)]

      (if (nil? attachment)
        (do
          (error "Attachment not found" {:id id :model-id model-id})
          (bad-request {:error "Attachment not found"}))

        ; If the accept header is application/octet-stream, return the file as a binary response


      (if (= accept-header "application/octet-stream")
        (->> base64-string
             (.decode (Base64/getMimeDecoder))
             (hash-map :body)
             (merge {:headers {"Content-Type" content-type
                               "Content-Transfer-Encoding" "binary"
                               "Content-Disposition" (str content-disposition "; filename=\"" file-name "\"")}}))
        ;(response {:attachments result})))
        (response attachment))))
    (catch Exception e
      (error "Failed to get attachments" e)
      (bad-request {:error "Failed to get attachments" :details (.getMessage e)}))))

(defn delete-resource [{:keys [tx] :as request}]
  (let [{:keys [attachments_id]} (path-params request)
        res (jdbc/execute-one! tx
                               (-> (sql/delete-from :attachments)
                                   (sql/where [:= :id attachments_id])
                                   sql-format))]
    (if (= (:next.jdbc/update-count res) 1)
      (response {:status "ok" :attachments_id attachments_id})
      (bad-request {:error "Failed to delete attachment"}))))
