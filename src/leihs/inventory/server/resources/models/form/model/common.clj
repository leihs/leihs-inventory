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
        p (println ">o> abc.image-data" image-data)

        ]

    (println ">o> abc >> INSERT IMAGE-ENTRY" )
    (jdbc/execute! tx (-> (sql/insert-into :images)
                        (sql/values [image-data])
                        (sql/returning :*)
                        sql-format)))
  )


  ;; new version for json-endpoint
(defn upload-image [req]

  (println ">o> upload-image" )

  ;(let [model_id (get-in req [:parameters :path :model_id])
  ;      body (get-in req [:parameters :body])]
  ;  (status (response {:model_id model_id :body body}) 200))


  (let [{{:keys [model-id]} :path} (:parameters req)
        ;; get the input stream from the Ring request
        body-stream (:body req)
        tx (:tx req)

        content-type (get-in req [:headers "content-type"])
        content-length (some-> (get-in req [:headers "content-length"]) Long/parseLong)

        p (println ">o> abc.body-stream" body-stream)

        filename-to-save "tmp-saved-upload.png"

        _ (io/copy body-stream (io/file filename-to-save))
        _ (println ">o> abc >> SAVED FILE TO DISK" filename-to-save)

        data (process-image tx {:tempfile filename-to-save} model-id )
        ]

  )  )


(defn sanitize-filename [filename]
  ;; Basic sanitization to prevent directory traversal
  (-> filename
    (str/replace #"[^a-zA-Z0-9_.-]" "_")))

  ;; new version for json-endpoint
(defn upload-attachment [req]
  (println ">o> upload-attachments" )

  (let [{{:keys [model_id]} :path} (:parameters req)
        ;; get the input stream from the Ring request
        body-stream (:body req)

        path (str (System/getProperty "user.dir") "/tmp/")
p (println ">o> path" path)

        ;tmp-dir ""

        p (println ">o> abc.header" (get-in req [:headers]))

        tx (:tx req)

        content-type (get-in req [:headers "content-type"])
        filename-to-save (get-in req [:headers "x-filename"])
        filename-to-save (sanitize-filename filename-to-save)
        content-length (some-> (get-in req [:headers "content-length"]) Long/parseLong)

        file-full-path (str path filename-to-save)
        ;filename-to-save "tmp-saved-upload.pdf"
        entry {:tempfile file-full-path :filename filename-to-save :content_type content-type :size content-length :model_id model_id}



        p (println ">o> abc.entry" entry)

        p (println ">o> abc.body-stream" body-stream)

        _ (io/copy body-stream (io/file file-full-path))
        _ (println ">o> abc >> SAVED FILE TO DISK" filename-to-save)

        ;data (process-image tx {:tempfile filename-to-save} model_id-id )

        ;(let [
              id (to-uuid model_id)
              file-content (file-to-base64 entry)

        ;p (println ">o> abc.file-content" file-content)

              data (assoc (dissoc entry :tempfile ) :content file-content )

        p (println ">o> attachemnts.data" data)

          data (jdbc/execute! tx (-> (sql/insert-into :attachments)
                              (sql/values [data])
                              (sql/returning :*)
                              sql-format))
         _ (println ">o> abc >> INSERTED IN DB")

        data {:foo "bar"}

        ]

    (status (response data) 200)
          )
  )



;(defn sanitize-filename [filename]
;  ;; Basic sanitization to prevent directory traversal
;  (-> filename
;    (str/replace #"[^a-zA-Z0-9_.-]" "_")))
;
;(defn upload-attachment [req]
;  (let [
;        tmp-dir ""
;
;        headers (:headers req)
;        body-stream (:body req)
;        raw-filename (get headers "x-filename")
;        filename (sanitize-filename raw-filename)
;        content-type (get headers "content-type")
;        content-length (some-> (get headers "content-length") Long/parseLong)
;        file (io/file tmp-dir filename)]
;
;    (with-open [in body-stream
;                out (io/output-stream file)]
;      (io/copy in out))
;
;    (-> {:filename filename
;         :status "saved"
;         :size content-length
;         :content-type content-type}
;      response
;      (status 200))))
