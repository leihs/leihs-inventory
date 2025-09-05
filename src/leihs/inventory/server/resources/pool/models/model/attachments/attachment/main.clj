(ns leihs.inventory.server.resources.pool.models.model.attachments.attachment.main
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.models.model.attachments.attachment.constants :refer [CONTENT_DISPOSITION_INLINE_FORMATS]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.helper :refer [log-by-severity]]
   [leihs.inventory.server.utils.request-utils :refer [path-params]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response]]
   [taoensso.timbre :refer [error]])
  (:import
   [java.util Base64]))

(def ERROR_GET_ATTACHMENT "Failed to get attachment")

(defn get-resource [request]
  (try
    (let [tx (:tx request)
          id (-> request path-params :attachments_id)
          model-id (-> request path-params :model_id)
          accept-header (get-in request [:headers "accept"])
          accept-header (if (= accept-header "text/html")
                          "*/*"
                          accept-header)
          json-request? (= accept-header "application/json")
          content-disposition (or (-> request :parameters :query :content_disposition) "inline")
          query (-> (sql/select :a.*)
                    (sql/from [:attachments :a])
                    (cond-> model-id (sql/where [:= :a.model_id model-id]))
                    (cond-> id (sql/where [:= :a.id id]))
                    (cond-> (and (not json-request?) (not "*/*"))
                      (sql/where [:= :a.content_type accept-header]))
                    sql-format)
          attachment (jdbc/execute-one! tx query)
          content-disposition (if (some #(= (:content_type attachment) %) CONTENT_DISPOSITION_INLINE_FORMATS)
                                content-disposition
                                "attachment")
          base64-string (:content attachment)
          file-name (:filename attachment)
          content-type (:content_type attachment)]

      (if (nil? attachment)
        (do
          (error "Attachment not found" {:id id :model-id model-id})
          (bad-request {:message "Attachment not found"}))
        (cond

          (= accept-header "application/json")
          (response attachment)

          :else (->> base64-string
                     (.decode (Base64/getMimeDecoder))
                     (hash-map :body)
                     (merge {:headers {"Content-Type" content-type
                                       "Content-Disposition" (str content-disposition "; filename=\"" file-name "\"")}})))))

    (catch Exception e
      (log-by-severity ERROR_GET_ATTACHMENT e)
      (exception-handler ERROR_GET_ATTACHMENT e))))

(defn delete-resource [{:keys [tx] :as request}]
  (let [{:keys [attachments_id]} (path-params request)
        res (jdbc/execute-one! tx
                               (-> (sql/delete-from :attachments)
                                   (sql/where [:= :id attachments_id])
                                   sql-format))]
    (if (= (:next.jdbc/update-count res) 1)
      (response {:status "ok" :attachments_id attachments_id})
      (bad-request {:message "Failed to delete attachment"}))))
