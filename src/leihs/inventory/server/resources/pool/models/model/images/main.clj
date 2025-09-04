(ns leihs.inventory.server.resources.pool.models.model.images.main
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.models.common :refer [filter-map-by-schema]]
   [leihs.inventory.server.resources.pool.models.model.constants :refer [config-get]]
   [leihs.inventory.server.resources.pool.models.model.images.types :as types]
   [leihs.inventory.server.utils.helper :refer [log-by-severity]]
   [leihs.inventory.server.utils.image-upload-handler :refer [file-to-base64
                                                              resize-and-convert-to-base64]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [next.jdbc :as jdbc]
   [ring.util.response :as response :refer [bad-request response status]]))

(defn sanitize-filename [filename]
  (str/replace filename #"[^a-zA-Z0-9_.-]" "_"))

(defn filter-keys [m keys-to-keep]
  (select-keys m keys-to-keep))

(defn filter-keys-images [m]
  (filter-keys m [:filename :content_type :size :thumbnail :target_id :target_type :parent_id :content]))

(defn post-resource [req]
  (try
    (let [{{:keys [model_id]} :path} (:parameters req)
          body-stream (:body req)
          allowed-file-types (config-get :api :images :allowed-file-types)
          max-size-mb (config-get :api :images :max-size-mb)
          upload-path (config-get :api :upload-dir)
          tx (:tx req)
          content-type (get-in req [:headers "content-type"])
          filename-to-save (sanitize-filename (get-in req [:headers "x-filename"]))
          content-length (some-> (get-in req [:headers "content-length"]) Long/parseLong)
          file-full-path (str upload-path filename-to-save)
          entry {:tempfile file-full-path :filename filename-to-save :content_type content-type :size content-length :model_id model_id}]

      (let [allowed-extensions allowed-file-types
            content-extension (last (clojure.string/split content-type #"/"))]
        (when-not (some #(= content-extension %) allowed-extensions)
          (throw (ex-info "Invalid file type" {:status 400 :error "Unsupported file type"}))))

      (when (> content-length (* max-size-mb 1024 1024))
        (throw (ex-info "File size exceeds limit" {:status 400 :error "File size exceeds limit"})))

      (io/copy body-stream (io/file file-full-path))

      (let [file-content-main (file-to-base64 entry)
            main-image-data (-> entry
                                (assoc :content file-content-main :target_id model_id :target_type "Model" :thumbnail false)
                                filter-keys-images)
            main-image-result (jdbc/execute-one! tx (-> (sql/insert-into :images)
                                                        (sql/values [main-image-data])
                                                        (sql/returning :*)
                                                        sql-format))
            main-image-result (filter-map-by-schema main-image-result types/image)

            thumb-data (resize-and-convert-to-base64 file-full-path)
            thumbnail-data (-> (assoc main-image-data :content (:base64 thumb-data) :size (:file-size thumb-data) :thumbnail true :parent_id (:id main-image-result))
                               filter-keys-images)
            thumbnail-result (jdbc/execute-one! tx (-> (sql/insert-into :images)
                                                       (sql/values [thumbnail-data])
                                                       (sql/returning :*)
                                                       sql-format))
            thumbnail-result (filter-map-by-schema thumbnail-result types/image)

            data (-> {:image main-image-result :thumbnail thumbnail-result :model_id model_id}
                     (filter-map-by-schema types/post-response))]
        (status (response data) 200)))

    (catch Exception e
      (log-by-severity "Failed to upload image" e)
      (bad-request {:error "Failed to upload image" :details (.getMessage e)}))))

(defn index-resources [request]
  (try
    (let [base-query (-> (sql/select :i.id :i.filename :i.target_id :i.size :i.thumbnail :i.content_type)
                         (sql/from [:images :i]))]
      (response (create-pagination-response request base-query nil)))
    (catch Exception e
      (log-by-severity "Failed to retrieve image:" e)
      (bad-request {:error "Failed to retrieve image" :details (.getMessage e)}))))
