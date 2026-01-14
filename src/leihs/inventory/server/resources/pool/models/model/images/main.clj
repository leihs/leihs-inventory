(ns leihs.inventory.server.resources.pool.models.model.images.main
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.middlewares.debug :refer [log-by-severity]]
   [leihs.inventory.server.middlewares.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.resources.pool.attachments.constants :refer [config-get]]
   [leihs.inventory.server.resources.pool.models.common :refer [filter-map-by-schema]]
   [leihs.inventory.server.resources.pool.models.model.images.types :as types]
   [leihs.inventory.server.utils.image :refer [file-to-base64
                                               resize-and-convert-to-base64]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [next.jdbc :as jdbc]
   [ring.util.response :as response :refer [bad-request response status]]))

(def ERROR_POST_IMAGE "Failed to upload image")

(defn sanitize-filename [filename]
  (str/replace filename #"[^a-zA-Z0-9_.-]" "_"))

(defn filter-keys [m keys-to-keep]
  (select-keys m keys-to-keep))

(defn filter-keys-images [m]
  (filter-keys m [:filename :content_type :size :thumbnail :target_id :target_type :parent_id :content]))

(defn post-resource [request]
  (try
    (let [{{:keys [model_id]} :path} (:parameters request)
          body-stream (:body request)
          allowed-file-types (config-get :api :images :allowed-file-types)
          max-size-mb (config-get :api :images :max-size-mb)
          upload-path (config-get :api :upload-dir)
          tx (:tx request)
          content-type (get-in request [:headers "content-type"])
          filename-to-save (sanitize-filename (get-in request [:headers "x-filename"]))
          content-length (some-> (get-in request [:headers "content-length"]) Long/parseLong)
          file-full-path (str upload-path filename-to-save)
          entry {:tempfile file-full-path :filename filename-to-save
                 :content_type content-type :size content-length :model_id model_id}

          allowed-extensions allowed-file-types
          content-extension (last (clojure.string/split content-type #"/"))]

      (cond
        (not (some #(= content-extension %) allowed-extensions))
        (bad-request {:status 400 :message "Unsupported file type"})

        (nil? content-length)
        (bad-request {:status 400 :message "File size not defined"})

        (> content-length (* max-size-mb 1024 1024))
        (bad-request {:status 400 :message "File size exceeds limit"})

        :else
        (let [_ (io/copy body-stream (io/file file-full-path))
              file-content-main (file-to-base64 entry)
              main-image-data (-> entry
                                  (assoc :content file-content-main
                                         :target_id model_id
                                         :target_type "Model"
                                         :thumbnail false)
                                  filter-keys-images)
              main-image-result (jdbc/execute-one! tx (-> (sql/insert-into :images)
                                                          (sql/values [main-image-data])
                                                          (sql/returning :*)
                                                          sql-format))
              main-image-result (filter-map-by-schema main-image-result types/image)

              thumb-data (resize-and-convert-to-base64 file-full-path)
              thumbnail-data (-> (assoc main-image-data
                                        :content (:base64 thumb-data)
                                        :size (:file-size thumb-data)
                                        :thumbnail true
                                        :parent_id (:id main-image-result))
                                 filter-keys-images)
              thumbnail-result (jdbc/execute-one! tx (-> (sql/insert-into :images)
                                                         (sql/values [thumbnail-data])
                                                         (sql/returning :*)
                                                         sql-format))
              thumbnail-result (filter-map-by-schema thumbnail-result types/image)

              data (-> {:image main-image-result
                        :thumbnail thumbnail-result
                        :model_id model_id}
                       (filter-map-by-schema types/post-response))]
          (status (response data) 200))))

    (catch Exception e
      (log-by-severity ERROR_POST_IMAGE e)
      (exception-handler request ERROR_POST_IMAGE e))))

(defn index-resources [request]
  (try
    (let [{{:keys [model_id]} :path} (:parameters request)
          base-query (-> (sql/select :i.id :i.filename :i.target_id :i.size :i.thumbnail :i.content_type)
                         (sql/from [:images :i])
                         (sql/where [:or [:= :i.parent_id model_id] [:= :i.target_id model_id]]))]
      (response (create-pagination-response request base-query nil)))
    (catch Exception e
      (log-by-severity "Failed to retrieve image:" e)
      (bad-request {:message "Failed to retrieve image" :details (.getMessage e)}))))
