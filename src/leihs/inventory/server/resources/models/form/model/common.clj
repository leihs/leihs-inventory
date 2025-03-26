(ns leihs.inventory.server.resources.models.form.model.common
  (:require
   [cheshire.core :as cjson]
   [clojure.data.codec.base64 :as b64]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.models.helper :refer [base-filename file-to-base64 normalize-files normalize-model-data
                                                           parse-json-array process-attachments str-to-bool file-sha256]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [next.jdbc :as jdbc]
   [pantomime.extract :as extract]
   [ring.util.response :as response]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]])
  (:import [java.net URL JarURLConnection]
           (java.time LocalDateTime)
           [java.util UUID]
           [java.util.jar JarFile]))

(defn update-image-attribute-ids [new-images-attr created-images]
  (vec (map (fn [image]
              (if-let [matching-entry (some #(when (= (:checksum image) (:checksum %)) %) created-images)]
                (assoc image :id (:id matching-entry))
                image))
            new-images-attr)))

(defn create-image-url [col-name col-name-keyword]
  [[[:raw (str "CASE WHEN " (name col-name) ".cover_image_id IS NOT NULL THEN CONCAT('/inventory/images/', " (name col-name) ".cover_image_id, '/thumbnail') ELSE NULL END")]]
   col-name-keyword])

(defn generate-thumbnail [a] a)

(defn add-thumb-to-filename [image-map]
  (update image-map :filename #(str (first (str/split % #"\.(?=[^.]+$)")) "_thumb." (second (str/split % #"\.(?=[^.]+$)")))))

(defn process-persist-images [tx images model-id validation-result]
  (reduce
   (fn [acc image]
     (let [tempfile (:tempfile image)
           checksum (file-sha256 image)
           file-content-main (file-to-base64 tempfile)
           main-image-data (-> (set/rename-keys image {:content-type :content_type})
                               (dissoc :tempfile)
                               (assoc :content file-content-main :target_id model-id :target_type "Model" :thumbnail false))
           main-image-result (jdbc/execute-one! tx (-> (sql/insert-into :images)
                                                       (sql/values [main-image-data])
                                                       (sql/returning :id :filename :thumbnail :size)
                                                       sql-format))
           main-image-result (assoc main-image-result :checksum checksum)
           file-content-thumb (generate-thumbnail file-content-main)
           main-image-data (add-thumb-to-filename main-image-data)
           thumbnail-data (assoc main-image-data :content file-content-thumb :thumbnail true :parent_id (:id main-image-result))
           thumbnail-result (jdbc/execute-one! tx (-> (sql/insert-into :images)
                                                      (sql/values [thumbnail-data])
                                                      (sql/returning :id :filename :thumbnail :size)
                                                      sql-format))]
       (conj acc main-image-result thumbnail-result)))
   []
   images))

(defn create-images-and-prepare-image-attributes [request]
  (let [images (normalize-files request :images)
        image-attributes (parse-json-array request :image_attributes)
        new-images-attr (filter #(contains? % :checksum) image-attributes)
        existing-images-attr (remove #(contains? % :checksum) image-attributes)]
    {:images images :image-attributes image-attributes :new-images-attr new-images-attr :existing-images-attr existing-images-attr}))

(defn prepare-image-attributes [tx images model-id validation-result new-images-attr existing-images-attr]
  (let [created-images-attr (process-persist-images tx images model-id validation-result)
        created-images-attr (update-image-attribute-ids new-images-attr created-images-attr)
        all-image-attributes (into existing-images-attr created-images-attr)]
    {:created-images-attr created-images-attr :all-image-attributes all-image-attributes}))

(defn sanitize-filename [filename]
  (str/replace filename #"[^a-zA-Z0-9_.-]" "_"))

(defn filter-keys [m keys-to-keep]
  (select-keys m keys-to-keep))

(defn filter-keys-images [m]
  (filter-keys m [:filename :content_type :size :thumbnail :target_id :target_type :parent_id :content]))

(defn filter-keys-attachments [m]
  (filter-keys m [:filename :content_type :size :model_id :item_id :content]))

(defn image-response-format [s]
  (sql/returning s :id :filename :thumbnail))

(defn attachment-response-format [s]
  (sql/returning s :id :filename))

(defn patch-models-handler [{{{:keys [model_id]} :path images-to-update :body} :parameters :as req}]
  (let [model-id (to-uuid model_id)
        {{:keys [pool_id]} :path} (:parameters req)
        tx (:tx req)
        results (mapv (fn [{:keys [id is_cover]}]
                        (when is_cover
                          (jdbc/execute-one! tx (-> (sql/update :models)
                                                    (sql/set {:cover_image_id (to-uuid is_cover)})
                                                    (sql/where [:= :id id])
                                                    (sql/returning :id :cover_image_id)
                                                    sql-format))))
                      images-to-update)]
    (response/response results)))

;(def CONST_FILE_PATH (str (System/getProperty "user.dir") "/tmp/"))
(def CONST_FILE_PATH "/tmp/")

(defn upload-image [req]
  (let [{{:keys [model_id]} :path} (:parameters req)
        body-stream (:body req)
        path CONST_FILE_PATH
        tx (:tx req)
        content-type (get-in req [:headers "content-type"])
        filename-to-save (sanitize-filename (get-in req [:headers "x-filename"]))
        content-length (some-> (get-in req [:headers "content-length"]) Long/parseLong)
        file-full-path (str path filename-to-save)
        entry {:tempfile file-full-path :filename filename-to-save :content_type content-type :size content-length :model_id model_id}]
    (io/copy body-stream (io/file file-full-path))
    (let [file-content-main (file-to-base64 entry)
          main-image-data (-> entry
                              (assoc :content file-content-main :target_id model_id :target_type "Model" :thumbnail false)
                              filter-keys-images)
          main-image-result (jdbc/execute-one! tx (-> (sql/insert-into :images)
                                                      (sql/values [main-image-data])
                                                      image-response-format
                                                      sql-format))
          file-content-thumb (generate-thumbnail file-content-main)
          main-image-data (add-thumb-to-filename main-image-data)
          thumbnail-data (-> (assoc main-image-data :content file-content-thumb :thumbnail true :parent_id (:id main-image-result))
                             filter-keys-images)
          thumbnail-result (jdbc/execute-one! tx (-> (sql/insert-into :images)
                                                     (sql/values [thumbnail-data])
                                                     image-response-format
                                                     sql-format))
          data {:image main-image-result :thumbnail thumbnail-result :model_id model_id}]
      (status (response data) 200))))

(defn upload-attachment [req]
  (let [{{:keys [model_id]} :path} (:parameters req)
        body-stream (:body req)
        path CONST_FILE_PATH
        tx (:tx req)
        content-type (get-in req [:headers "content-type"])
        filename-to-save (sanitize-filename (get-in req [:headers "x-filename"]))
        content-length (some-> (get-in req [:headers "content-length"]) Long/parseLong)
        file-full-path (str path filename-to-save)
        entry {:tempfile file-full-path :filename filename-to-save :content_type content-type :size content-length :model_id model_id}]
    (io/copy body-stream (io/file file-full-path))
    (let [file-content (file-to-base64 entry)
          data (-> entry
                   (assoc :content file-content)
                   filter-keys-attachments)
          data (jdbc/execute! tx (-> (sql/insert-into :attachments)
                                     (sql/values [data])
                                     attachment-response-format
                                     sql-format))]
      (status (response data) 200))))

(defn prepare-model-data
  [data]
  (let [normalize-data (normalize-model-data data)
        created-ts (LocalDateTime/now)
        normalize-data (dissoc normalize-data :id)]
    (assoc normalize-data
           :type "Model"
           :created_at created-ts
           :updated_at created-ts)))

(defn extract-model-form-data [request create-all]
  (let [multipart (or (get-in request [:parameters :multipart])
                      (get-in request [:parameters :body]))
        prepared-model-data (-> (prepare-model-data multipart)
                                (assoc :is_package (str-to-bool (:is_package multipart))))
        categories (parse-json-array multipart :categories)
        compatibles (parse-json-array multipart :compatibles)
        properties (parse-json-array multipart :properties)
        accessories (parse-json-array multipart :accessories)
        entitlements (parse-json-array multipart :entitlements)
        attachments (when create-all (normalize-files request :attachments)) ; maybe FIXME
        attachments-to-delete (parse-json-array multipart :attachments_to_delete)
        images-to-delete (parse-json-array multipart :images_to_delete)
        {:keys [images image-attributes new-images-attr existing-images-attr]}
        (when create-all (create-images-and-prepare-image-attributes request))]
    {:prepared-model-data prepared-model-data
     :categories categories
     :compatibles compatibles
     :properties properties
     :accessories accessories
     :entitlements entitlements
     :attachments attachments
     :attachments-to-delete attachments-to-delete
     :images-to-delete images-to-delete
     :images images
     :new-images-attr new-images-attr
     :existing-images-attr existing-images-attr}))
