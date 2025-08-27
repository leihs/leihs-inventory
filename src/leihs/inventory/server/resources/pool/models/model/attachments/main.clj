(ns leihs.inventory.server.resources.pool.models.model.attachments.main
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.models.model.constants :refer [config-get]]
   [leihs.inventory.server.utils.image-upload-handler :refer [file-to-base64]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [leihs.inventory.server.utils.request-utils :refer [path-params]]
   [next.jdbc :as jdbc]
   [ring.util.response :as response :refer [bad-request response status]]
   [taoensso.timbre :refer [debug error]]))

(defn sanitize-filename [filename]
  (str/replace filename #"[^a-zA-Z0-9_.-]" "_"))

(defn filter-keys [m keys-to-keep]
  (select-keys m keys-to-keep))

(defn filter-keys-attachments [m]
  (filter-keys m [:filename :content_type :size :model_id :item_id :content]))

(defn attachment-response-format [s]
  (sql/returning s :id :filename :content_type :size :model_id :item_id))

(defn post-resource [req]
  (try
    (let [{{:keys [model_id]} :path} (:parameters req)
          body-stream (:body req)
          max-size-mb (config-get :api :attachments :max-size-mb)
          upload-path (config-get :api :upload-dir)
          tx (:tx req)
          content-type (get-in req [:headers "content-type"])
          filename-to-save (sanitize-filename (get-in req [:headers "x-filename"]))
          content-length (some-> (get-in req [:headers "content-length"]) Long/parseLong)
          file-full-path (str upload-path filename-to-save)
          entry {:tempfile file-full-path
                 :filename filename-to-save
                 :content_type content-type
                 :size content-length
                 :model_id model_id}]

      (when (> content-length (* max-size-mb 1024 1024))
        (throw (ex-info "File size exceeds limit" {:status 400 :error "File size exceeds limit"})))

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
      (debug e)
      (error "Failed to upload attachment" e)
      (bad-request {:error "Failed to upload attachment" :details (.getMessage e)}))))

(defn index-resources [request]
  (try
    (let [model-id (-> request path-params :model_id)
          query (-> (sql/select :a.*)
                    (sql/from [:attachments :a])
                    (cond-> model-id (sql/where [:= :a.model_id model-id])))]
      (response (create-pagination-response request query nil)))
    (catch Exception e
      (debug e)
      (error "Failed to get attachments" e)
      (bad-request {:error "Failed to get attachments" :details (.getMessage e)}))))
