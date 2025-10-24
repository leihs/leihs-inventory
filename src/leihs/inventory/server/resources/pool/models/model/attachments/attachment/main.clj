(ns leihs.inventory.server.resources.pool.models.model.attachments.attachment.main
  (:require
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.models.model.attachments.attachment.constants :refer [CONTENT_DISPOSITION_INLINE_FORMATS]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.helper :refer [log-by-severity]]
   [leihs.inventory.server.utils.request-utils :refer [path-params]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response]])
  (:import
   [java.util Base64]))

(def ERROR_GET_ATTACHMENT "Failed to get attachment")

(defn get-resource [request]
  (try
    (let [tx (:tx request)
          id (-> request path-params :attachments_id)
          model-id (-> request path-params :model_id)
          accept-header (if (str/includes? (get-in request [:headers "accept"]) "*/*")
                          "*/*"
                          (get-in request [:headers "accept"]))
          content-negotiation? (str/includes? accept-header "*/*")
          json-request? (= accept-header "application/json")
          content-disposition (or (-> request :parameters :query :content_disposition) "inline")
          query (-> (sql/select :a.*)
                    (sql/from [:attachments :a])
                    (cond-> model-id (sql/where [:= :a.model_id model-id]))
                    (cond-> id (sql/where [:= :a.id id]))
                    sql-format)
          attachment (jdbc/execute-one! tx query)
          content-type (:content_type attachment)]

      (cond
        (nil? attachment) (throw (ex-info "No attachment found" {:status 404}))

        json-request? (response attachment)

        (and (not content-negotiation?) (not= content-type accept-header))
        (throw (ex-info "No attachment found for the requested content type" {:status 406}))

        :else
        (let [content-disposition (if (some #(= content-type %) CONTENT_DISPOSITION_INLINE_FORMATS)
                                    content-disposition
                                    "attachment")
              base64-string (:content attachment)
              file-name (:filename attachment)]
          (->> base64-string
               (.decode (Base64/getMimeDecoder))
               (hash-map :body)
               (merge {:headers {"Content-Type" content-type
                                 "Content-Disposition" (str content-disposition "; filename=\"" file-name "\"")}})))))

    (catch Exception e
      (log-by-severity ERROR_GET_ATTACHMENT e)
      (exception-handler request ERROR_GET_ATTACHMENT e))))

(defn delete-resource [{:keys [tx] :as request}]
  (let [{:keys [attachments_id]} (path-params request)
        res (jdbc/execute-one! tx
                               (-> (sql/delete-from :attachments)
                                   (sql/where [:= :id attachments_id])
                                   sql-format))]
    (if (= (:next.jdbc/update-count res) 1)
      (response {:status "ok" :attachments_id attachments_id})
      (bad-request {:message "Failed to delete attachment"}))))
