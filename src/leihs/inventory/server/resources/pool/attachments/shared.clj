(ns leihs.inventory.server.resources.pool.attachments.shared
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.attachments.constants :refer [config-get CONTENT_DISPOSITION_INLINE_FORMATS]]
   [leihs.inventory.server.utils.debug :refer [log-by-severity]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.image-upload-handler :refer [file-to-base64]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [leihs.inventory.server.utils.request-utils :refer [path-params]]
   [next.jdbc :as jdbc]
   [ring.util.response :as response :refer [bad-request response status]])
  (:import
   [java.util Base64]))

(def ERROR_GET_ATTACHMENT "Failed to get attachment")

(defn get-resource [request]
  (try
    (let [tx (:tx request)
          id (-> request path-params :attachment_id)
          model-id (-> request path-params :model_id)
          item-id (-> request path-params :item_id)
          accept-header (if (str/includes? (get-in request [:headers "accept"]) "*/*")
                          "*/*"
                          (get-in request [:headers "accept"]))
          content-negotiation? (str/includes? accept-header "*/*")
          json-request? (= accept-header "application/json")
          content-disposition (or (-> request :parameters :query :content_disposition) "inline")
          query (-> (sql/select :a.*)
                    (sql/from [:attachments :a])
                    (cond->
                     model-id (sql/where [:= :a.model_id model-id])
                     item-id (sql/where [:= :a.item_id item-id]))
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
  (let [{:keys [attachment_id]} (path-params request)
        res (jdbc/execute-one! tx
                               (-> (sql/delete-from :attachments)
                                   (sql/where [:= :id attachment_id])
                                   sql-format))]
    (if (= (:next.jdbc/update-count res) 1)
      (response {:status "ok" :attachment_id attachment_id})
      (bad-request {:message "Failed to delete attachment"}))))

(def ERROR_UPLOAD_ATTACHMENT "Failed to upload attachment")
(def ERROR_GET_ATTACHMENTS "Failed to fetch thumbnails")

(defn sanitize-filename [filename]
  (str/replace filename #"[^a-zA-Z0-9_.-]" "_"))

(defn filter-keys [m keys-to-keep]
  (select-keys m keys-to-keep))

(defn filter-keys-attachments [m]
  (filter-keys m [:filename :content_type :size :model_id :item_id :content]))

(defn attachment-response-format [s]
  (sql/returning s :id :filename :content_type :size :model_id :item_id))

(defn post-resource [request]
  (try
    (let [{{:keys [item_id model_id]} :path} (:parameters request)
          body-stream (:body request)
          max-size-mb (config-get :api :attachments :max-size-mb)
          upload-path (config-get :api :upload-dir)
          tx (:tx request)
          content-type (get-in request [:headers "content-type"])
          filename-to-save (sanitize-filename (get-in request [:headers "x-filename"]))
          content-length (some-> (get-in request [:headers "content-length"]) Long/parseLong)
          file-full-path (str upload-path filename-to-save)
          entry {:tempfile file-full-path
                 :filename filename-to-save
                 :content_type content-type
                 :size content-length
                 :model_id model_id
                 :item_id item_id}]

      (when (> content-length (* max-size-mb 1024 1024))
        (throw (ex-info "File size exceeds limit" {:status 400 :message "File size exceeds limit"})))

      (io/copy body-stream (io/file file-full-path))

      (let [file-content (file-to-base64 entry)
            data (-> entry
                     (assoc :content file-content)
                     filter-keys-attachments)
            data (jdbc/execute-one! tx (-> (sql/insert-into :attachments)
                                           (sql/values [data])
                                           attachment-response-format
                                           sql-format))]
        (status (response data) 200)))

    (catch Exception e
      (log-by-severity ERROR_UPLOAD_ATTACHMENT e)
      (exception-handler request ERROR_UPLOAD_ATTACHMENT e))))

(defn index-resources [request]
  (try
    (let [item-id (-> request path-params :item_id)
          model-id (-> request path-params :model_id)
          query (-> (sql/select :a.*)
                    (sql/from [:attachments :a])
                    (cond-> model-id
                      (sql/where [:and
                                  [:= :a.model_id model-id]
                                  [:= :a.item_id item-id]])))]
      (response (create-pagination-response request query nil)))
    (catch Exception e
      (log-by-severity ERROR_GET_ATTACHMENTS e)
      (exception-handler request ERROR_GET_ATTACHMENTS e))))
