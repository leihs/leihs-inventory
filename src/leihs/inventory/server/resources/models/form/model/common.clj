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
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]])
  (:import [java.net URL JarURLConnection]
           (java.time LocalDateTime)
           [java.util UUID]
           [java.util.jar JarFile]))

(defn update-image-attribute-ids [new-images-attr created-images]
  (vec (map (fn [image]
              (let [matching-entry (some #(when (= (:checksum image)
                                                   (:checksum %))
                                            %)
                                         created-images)]
                (if matching-entry
                  (assoc image :id (:id matching-entry))
                  image)))
            new-images-attr)))

(defn create-image-url [col-name col-name-keyword]
  (let [col-name (name col-name)]

    [[[:raw (str "CASE
                                       WHEN " col-name ".cover_image_id IS NOT NULL
                                       THEN CONCAT('/inventory/images/', " col-name ".cover_image_id, '/thumbnail')
                                       ELSE NULL
                                    END")]]
     col-name-keyword]))

(defn generate-thumbnail [a] a)

(defn add-thumb-to-filename [image-map]
  (update image-map :filename
          (fn [filename]
            (let [[name ext] (clojure.string/split filename #"\.(?=[^.]+$)")]
              (str name "_thumb." ext)))))

(defn process-persist-images [tx images model-id validation-result]
  (reduce
   (fn [acc image]
     (let [tempfile (:tempfile image)
           checksum (file-sha256 image)
           file-content-main (file-to-base64 tempfile)
           main-image-data (-> (set/rename-keys image {:content-type :content_type})
                               (dissoc :tempfile)
                               (assoc :content file-content-main
                                      :target_id model-id
                                      :target_type "Model"
                                      :thumbnail false))
           main-image-result (jdbc/execute-one! tx (-> (sql/insert-into :images)
                                                       (sql/values [main-image-data])
                                                       (sql/returning :id :filename :thumbnail :size)
                                                       sql-format))
           main-image-result (assoc main-image-result :checksum checksum)
           file-content-thumb (generate-thumbnail file-content-main)
           main-image-data (add-thumb-to-filename main-image-data)
           thumbnail-data (assoc main-image-data
                                 :content file-content-thumb
                                 :thumbnail true
                                 :parent_id (:id main-image-result))
           thumbnail-result (jdbc/execute-one! tx (-> (sql/insert-into :images)
                                                      (sql/values [thumbnail-data])
                                                      (sql/returning :id :filename :thumbnail :size)
                                                      sql-format))]
       (into (or acc []) [main-image-result thumbnail-result])))
   []
   images))

(defn create-images-and-prepare-image-attributes "Create image-entry and update image-attributes" [request]
  (let [images (normalize-files request :images)
        image-attributes (parse-json-array request :image_attributes)
        new-images-attr (vec (filter #(contains? % :checksum) image-attributes))
        existing-images-attr (vec (filter #(not (contains? % :checksum)) image-attributes))]
    {:images images
     :image-attributes image-attributes
     :new-images-attr new-images-attr
     :existing-images-attr existing-images-attr}))

(defn prepare-image-attributes [tx images model-id validation-result new-images-attr existing-images-attr]
  (let [created-images-attr (process-persist-images tx images model-id validation-result)
        created-images-attr (update-image-attribute-ids new-images-attr created-images-attr)

        all-image-attributes (into existing-images-attr created-images-attr)]
    {:created-images-attr created-images-attr
     :all-image-attributes all-image-attributes}))
