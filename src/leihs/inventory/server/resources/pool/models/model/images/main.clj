(ns leihs.inventory.server.resources.pool.models.model.images.main
  (:require
   [cheshire.core :as cjson]
   [clojure.data.codec.base64 :as b64]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.java.shell :refer [sh]]
   [clojure.set :as set]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.constants :refer [config-get]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.image-upload-handler :refer [file-to-base64 resize-and-convert-to-base64]]
   [leihs.inventory.server.utils.pagination :refer [create-paginated-response fetch-pagination-params]]
   [next.jdbc :as jdbc]
   [pantomime.extract :as extract]
   [ring.util.response :as response :refer [bad-request response status]]
   [taoensso.timbre :refer [error spy]]))

(defn sanitize-filename [filename]
  (str/replace filename #"[^a-zA-Z0-9_.-]" "_"))

(defn filter-keys [m keys-to-keep]
  (select-keys m keys-to-keep))

(defn filter-keys-images [m]
  (filter-keys m [:filename :content_type :size :thumbnail :target_id :target_type :parent_id :content]))

(defn image-response-format [s]
  (sql/returning s :id :filename :thumbnail))

(defn delete-image
  [req]
  (let [tx (:tx req)
        {:keys [model_id image_id]} (:path (:parameters req))
        id (to-uuid image_id)]
    (let [res (jdbc/execute-one! tx
                                 (sql-format
                                  {:delete-from :images :where [:= :id id]}))]
      (if (= (:next.jdbc/update-count res) 1)
        (response {:status "ok" :image_id image_id})
        (bad-request {:error "Failed to delete image"})))))

(defn upload-image [req]
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
                                                        image-response-format
                                                        sql-format))

            thumb-data (resize-and-convert-to-base64 file-full-path)

            thumbnail-data (-> (assoc main-image-data :content (:base64 thumb-data) :size (:file-size thumb-data) :thumbnail true :parent_id (:id main-image-result))
                               filter-keys-images)
            thumbnail-result (jdbc/execute-one! tx (-> (sql/insert-into :images)
                                                       (sql/values [thumbnail-data])
                                                       image-response-format
                                                       sql-format))
            data {:image main-image-result :thumbnail thumbnail-result :model_id model_id}]
        (status (response data) 200)))

    (catch Exception e
      (error "Failed to upload image" e)
      (bad-request {:error "Failed to upload image" :details (.getMessage e)}))))

(defn validate-empty-string!
  ([k vec-of-maps]
   (validate-empty-string! k vec-of-maps nil))
  ([k vec-of-maps scope]
   (doseq [m vec-of-maps]
     (when (and (contains? m k) (= "" (get m k)))
       (throw (ex-info (str "Field '" k "' cannot be an empty string.")
                       (merge {:key k :map m} (when scope {:scope scope}))))))))
(defn get-images [request]
  (try
    (let [tx (:tx request)
          accept-header (get-in request [:headers "accept"])
          json-request? (= accept-header "application/json")
          base-query (-> (sql/select :i.id :i.filename :i.target_id :i.size :i.thumbnail)
                         (sql/from [:images :i]))]
      (let [{:keys [page size]} (fetch-pagination-params request)]
        (response (create-paginated-response base-query tx size page))))
    (catch Exception e
      (error "Failed to retrieve image:" (.getMessage e))
      (bad-request {:error "Failed to retrieve image" :details (.getMessage e)}))))
