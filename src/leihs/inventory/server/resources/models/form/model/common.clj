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

   ;[leihs.inventory.server.resources.models.form.model.model-by-pool-form-update :refer [
   ;                                                                                      ;delete-model-handler-by-pool-form
   ;                                                                                      process-image
   ;                                                                                      ;update-model-handler-by-pool-form
   ;                                                                                      ]]

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

(defn process-persist-images
  "Convert file to base64, insert image-entry and thumbnail-entry"
  [tx images model-id validation-result]
  (reduce
   (fn [acc image]
     (let [tempfile (:tempfile image)
           checksum (file-sha256 image)
           ;; insert image-entry
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
           ;; insert thumb-entry
           main-image-data (add-thumb-to-filename main-image-data)
           thumbnail-data (assoc main-image-data
                                 :content file-content-thumb
                                 :thumbnail true
                                 :parent_id (:id main-image-result))
           thumbnail-result (jdbc/execute-one! tx (-> (sql/insert-into :images)
                                                      (sql/values [thumbnail-data])
                                                      (sql/returning :id :filename :thumbnail :size)
                                                      sql-format))

           ]
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

(defn prepare-image-attributes
  "Insert image-entries and image-attributes, prepare image-attributes for update
  -
  "
  [tx images model-id validation-result new-images-attr existing-images-attr]
  (let [created-images-attr (process-persist-images tx images model-id validation-result)
        created-images-attr (update-image-attribute-ids new-images-attr created-images-attr)

        all-image-attributes (into existing-images-attr created-images-attr)]
    {:created-images-attr created-images-attr
     :all-image-attributes all-image-attributes}))

(defn process-image [tx image model-id]

  (let [file-content (file-to-base64 (:tempfile image))
        image-data (-> (set/rename-keys image {:content-type :content_type})
                       (dissoc :tempfile)
                       (assoc :content file-content
                              :target_id model-id
                              :target_type "Model"
                              :thumbnail false))
        p (println ">o> abc.image-data" image-data)]

    (println ">o> abc >> INSERT IMAGE-ENTRY")
    (jdbc/execute! tx (-> (sql/insert-into :images)
                          (sql/values [image-data])
                          (sql/returning :*)
                          sql-format))))




(defn sanitize-filename [filename]
  (str/replace filename #"[^a-zA-Z0-9_.-]" "_"))

(defn filter-keys
  [m keys-to-keep]
  (select-keys m keys-to-keep))

(defn filter-keys-images
  [m]
   (filter-keys m [:filename :content_type :size  :thumbnail :target_id :target_type :parent_id :content]))

(defn filter-keys-attachments
  [m]
   (filter-keys m [:filename :content_type :size  :model_id :item_id :content ]))


(defn image-response-format [s]
  (-> s
    ;(sql/returning :id :filename :thumbnail :size))
    (sql/returning :id :filename :thumbnail))
  )

(defn attachment-response-format [s]
  (-> s
    ;(sql/returning :id :filename :content_type)
    (sql/returning :id :filename)

    )
  )


;; new version for json-endpoint
(defn upload-image [req]

  (println ">o> upload-image")

  (let [{{:keys [model_id]} :path} (:parameters req)
        body-stream (:body req)
        path (str (System/getProperty "user.dir") "/tmp/")
        tx (:tx req)
        content-type (get-in req [:headers "content-type"])

        filename-to-save (sanitize-filename (get-in req [:headers "x-filename"]))
        content-length (some-> (get-in req [:headers "content-length"]) Long/parseLong)
        file-full-path (str path filename-to-save)
        entry {:tempfile file-full-path :filename filename-to-save :content_type content-type :size content-length
               :model_id model_id
               }]
    (println ">o> path" path)
    (println ">o> abc.entry" entry)
    ;(io/copy body-stream (io/file file-full-path))
    (println ">o> abc >> SAVED FILE TO DISK" filename-to-save)



  (let [
        ;tempfile (:tempfile entry)
        ;checksum (file-sha256 image)
        ;; insert image-entry
        ;file-content-main (file-to-base64 tempfile)

        p (println ">o> abc.before")

        ;file-content-main (file-to-base64 file-full-path)
        file-content-main (file-to-base64 entry)
        p (println ">o> abc.after" entry)

        main-image-data (->
                          entry
                          ;(set/rename-keys entry {:content-type :content_type})
                          ;(dissoc :tempfile :model_id)
                          (assoc :content file-content-main
                            :target_id model_id
                            :target_type "Model"
                            :thumbnail false)
                          filter-keys-images
                          )

        p (println ">o> abc.main-image-data"  (dissoc main-image-data :content))

        main-image-result (jdbc/execute-one! tx (-> (sql/insert-into :images)
                                                  (sql/values [main-image-data])
                                                  ;(sql/returning :id :filename :thumbnail :size)
                                                  image-response-format
                                                  sql-format))
        ;main-image-result (assoc main-image-result :checksum checksum)
        file-content-thumb (generate-thumbnail file-content-main)

        ;; insert thumb-entry
        main-image-data (add-thumb-to-filename main-image-data)
        thumbnail-data (-> (assoc main-image-data
                         :content file-content-thumb
                         :thumbnail true
                         :parent_id (:id main-image-result))
                         filter-keys-images
                         )
        p (println ">o> abc.thumbnail-data" (dissoc thumbnail-data :content))

        thumbnail-result (jdbc/execute-one! tx (-> (sql/insert-into :images)
                                                 (sql/values [thumbnail-data])
                                                 ;(sql/returning :id :filename :thumbnail :size)
                                                 image-response-format
                                                 sql-format))


        ;data {:image main-image-result :thumbnail thumbnail-result}
        data {:image main-image-result
              :thumbnail thumbnail-result
              :model_id model_id
              }

        ;>o> >>> abc.images.data {:image {:id #uuid "bc6d40da-ad2b-455d-843e-2209bc585ba9", :filename upload2_thumb.png,
        ; :thumbnail false, :size 37552}, :thumbnail {:id #uuid "3a40ec5f-86ef-47bc-a909-079ad42ec586",
        ; :filename upload2_thumb_thumb.png, :thumbnail true, :size 37552}}

        p (println ">o> >>> abc.images.data" data)
        ]

    (println ">o> abc >> INSERTED IN DB")
    (status (response data) 200))
  ))





(defn upload-attachment [req]
  (println ">o> upload-attachments")
  (let [{{:keys [model_id]} :path} (:parameters req)
        body-stream (:body req)
        path (str (System/getProperty "user.dir") "/tmp/")
        tx (:tx req)
        content-type (get-in req [:headers "content-type"])

        filename-to-save (sanitize-filename (get-in req [:headers "x-filename"]))
        content-length (some-> (get-in req [:headers "content-length"]) Long/parseLong)
        file-full-path (str path filename-to-save)
        entry {:tempfile file-full-path :filename filename-to-save :content_type content-type :size content-length :model_id model_id}]

    (println ">o> path" path)
    (println ">o> abc.entry" entry)
    (io/copy body-stream (io/file file-full-path))
    (println ">o> abc >> SAVED FILE TO DISK" filename-to-save)

    (let [id (to-uuid model_id)
          file-content (file-to-base64 entry)
          ;data (assoc (dissoc entry :tempfile) :content file-content)
          data (-> entry
                 (assoc :content file-content)
                 filter-keys-attachments)

          data (jdbc/execute! tx (-> (sql/insert-into :attachments)
                                     (sql/values [data])
                                     ;(sql/returning :id :filename :content_type :size :item_id)
                                     ;(sql/returning :id :filename :content_type)
                                   attachment-response-format
                                     sql-format))]


      ; abc.attachments.data [{:id #uuid "2155e275-9fb7-447f-87c7-3b939c1fc145", :filename upload_thumb.pdf,
      ; :content_type application/pdf, :size 17234, :item_id nil}]

      (println ">o> >>> abc.attachments.data" data)


      (println ">o> abc >> INSERTED IN DB")
      (status (response data) 200))))




