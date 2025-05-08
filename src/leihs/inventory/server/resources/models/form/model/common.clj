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
   [leihs.inventory.server.resources.models.helper :refer [base-filename
                                                           ;file-to-base64
                                                           normalize-files normalize-model-data
                                                           parse-json-array process-attachments str-to-bool file-sha256]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [next.jdbc :as jdbc]
   [pantomime.extract :as extract]
   [ring.util.response :as response]
   [ring.util.response :refer [bad-request response status]]

   ;[clojure.java.io :as io]
   ;[clojure.data.codec.base64 :as b64]
   [clojure.java.shell :refer [sh]]
    [clojure.java.io :as io]

   [taoensso.timbre :refer [error]])
  (:import [java.net URL JarURLConnection]
   [org.im4java.core ConvertCmd IMOperation]
           (java.time LocalDateTime)
   [java.io File FileInputStream ByteArrayOutputStream]
           [java.util UUID]
   [java.util Base64]
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

(defn generate-thumbnail [base64 path]


  ;; generate thumbnail logic here

  )

(defn generate-thumbnail [base64 original-path ]
  (let [
        ;original-path (str path "/original.jpg")
        ;thumbnail-path (str path "/thumbnail.jpg")
        thumbnail-path (str original-path ".thumb")

        p (println ">o> abc.orig" original-path)
        p (println ">o> abc.thumb" thumbnail-path)


        ]
    ;(base64-to-file base64 original-path)
    (let [result (try
                   (let [process (.exec (Runtime/getRuntime) (str "convert " original-path " -resize 100x100 " thumbnail-path))]
                     (.waitFor process)
                     (.exists (io/file thumbnail-path)))
                   (catch Exception e
                     (println "Error generating thumbnail:" (.getMessage e))
                     false))]
      (if result
        thumbnail-path
        (throw (Exception. "Could not generate thumbnail"))))))


(defn base64-to-file [base64-str filepath]
  (with-open [out (io/output-stream filepath)]
    (.write out (b64/decode (.getBytes base64-str)))))

;(defn file-to-base64 [filepath]
;  (with-open [in (io/input-stream filepath)]
;    (let [bytes (byte-array (.available in))]
;      (.read in bytes)
;      (String. (b64/encode bytes)))))

;(defn generate-thumbnail [original-path thumbnail-path]
;    (println ">o> generate-thumbnail1" original-path)
;    (println ">o> generate-thumbnail2" thumbnail-path)
;    (let [result (try
;                   (let [process (.exec (Runtime/getRuntime) (str "convert " original-path " -resize 100x100 " thumbnail-path))]
;                     (.waitFor process)
;                     (.exists (io/file thumbnail-path)))
;                   (catch Exception e
;                     (println "Error generating thumbnail:" e)
;                     false))]
;      (if result
;        (file-to-base64 thumbnail-path)
;        (throw (Exception. "Could not generate thumbnail")))))

(defn pr [str fnc]
  ;(println ">oo> HELPER / " str fnc)(println ">oo> HELPER / " str fnc)
  (println ">oo> " str fnc)
  fnc
  )

(defn file-to-base642 [file]
  (let [actual-file (cond
                      (instance? java.io.File file) file
                      (map? file) (:tempfile file)
                      :else nil)]
    (if actual-file
      (try
        (with-open [in (io/input-stream actual-file)
                    out (java.io.ByteArrayOutputStream.)]
          (io/copy in out)
          (let [encoded-bytes (b64/encode (.toByteArray out))]
            (str (String. encoded-bytes "UTF-8"))))
        (catch Exception e
          (println "Error encoding file to Base64:" (.getMessage e))
          nil))
      (do
        (println "Invalid file reference provided to file-to-base64 function.")
        nil))))



(defn generate-thumbnail [original-path thumbnail-path]
  (println ">o> generate-thumbnail1" original-path)
  (println ">o> generate-thumbnail2" thumbnail-path)
  (try
    (let [process (.exec (Runtime/getRuntime)
                    (str "convert " original-path " -resize 100x100 " thumbnail-path))
          exit-code (.waitFor process)]
      (if (zero? exit-code)
        (if (.exists (io/file thumbnail-path))
          (pr "> new-thumb" (file-to-base642 thumbnail-path))
          (throw (Exception. "Thumbnail file was not created.")))
        (let [error-stream (.getErrorStream process)
              error-msg (slurp error-stream)]
          (throw (Exception. (str "ImageMagick convert failed: " error-msg))))))
    (catch Exception e
      (println "Error generating thumbnail:" (.getMessage e))
      nil)))

;(defn generate-thumbnail [original-path thumbnail-path]
;(println ">o> generate-thumbnail1" original-path)
;(println ">o> generate-thumbnail2" thumbnail-path)
;(try
;  (let [process (.exec (Runtime/getRuntime)
;                  (str "convert " original-path " -resize 100x100 " thumbnail-path))
;        exit-code (.waitFor process)]
;    ;(Thread/sleep 2000)  ;; Wait for 2 seconds before checking the file
;    (if (zero? exit-code)
;      (if (.exists (io/file thumbnail-path))
;        (pr "> new-thumb" (file-to-base64 (pr ">path ????" thumbnail-path)))
;        (throw (Exception. "Thumbnail file was not created.")))
;      (let [error-stream (.getErrorStream process)
;            error-msg (slurp error-stream)]
;        (throw (Exception. (str "ImageMagick convert failed: " error-msg))))))
;  (catch Exception e
;    (println "Error generating thumbnail:" (.getMessage e))
;    nil)))


;(defn generate-thumbnail [original-path thumbnail-path]
;  (println ">o> Checking access rights for paths")
;  (println ">o> Original path exists? " (.exists (io/file original-path)))
;  (println ">o> Original path readable? " (.canRead (io/file original-path)))
;  (println ">o> Thumbnail path writable? " (.canWrite (io/file (io/file thumbnail-path))))
;
;  (try
;    (let [process (.exec (Runtime/getRuntime)
;                    (str "convert " original-path " -resize 100x100 " thumbnail-path))
;          exit-code (.waitFor process)
;          error-output (slurp (.getErrorStream process))]
;
;      (when-not (zero? exit-code)
;        (println "Error during convert operation:" error-output)
;        (throw (Exception. (str "Convert command failed with exit code " exit-code ": " error-output))))
;
;      (if (.exists (io/file thumbnail-path))
;        (file-to-base64 thumbnail-path)
;        (throw (Exception. "Thumbnail file was not created."))))
;
;    (catch Exception e
;      (println "Error generating thumbnail:" (.getMessage e))
;      nil)))

(defn add-thumb-to-filename [image-map]
  (update image-map :filename #(str (first (str/split % #"\.(?=[^.]+$)")) "_thumb." (second (str/split % #"\.(?=[^.]+$)")))))

;(defn process-persist-images [tx images model-id validation-result]
;  (reduce
;   (fn [acc image]
;     (let [tempfile (:tempfile image)
;
;           p (println ">o> abc.tempfile >> " tempfile)
;           checksum (file-sha256 image)
;           file-content-main (file-to-base64 tempfile)
;           main-image-data (-> (set/rename-keys image {:content-type :content_type})
;                               (dissoc :tempfile)
;                               (assoc :content file-content-main :target_id model-id :target_type "Model" :thumbnail false))
;           main-image-result (jdbc/execute-one! tx (-> (sql/insert-into :images)
;                                                       (sql/values [main-image-data])
;                                                       (sql/returning :id :filename :thumbnail :size)
;                                                       sql-format))
;           main-image-result (assoc main-image-result :checksum checksum)
;           p (println ">o> abc.main-image-data.before" main-image-data)
;
;
;           main-image-data (add-thumb-to-filename main-image-data)
;           p (println ">o> abc.main-image-data.after" main-image-data)
;
;           file-content-thumb (generate-thumbnail file-content-main tempfile (str "/tmp/" (:filename main-image-data)))
;
;
;           thumbnail-data (assoc main-image-data :content file-content-thumb :thumbnail true :parent_id (:id main-image-result))
;           thumbnail-result (jdbc/execute-one! tx (-> (sql/insert-into :images)
;                                                      (sql/values [thumbnail-data])
;                                                      (sql/returning :id :filename :thumbnail :size)
;                                                      sql-format))]
;       (conj acc main-image-result thumbnail-result)))
;   []
;   images))

(defn create-images-and-prepare-image-attributes [request]
  (let [images (normalize-files request :images)
        image-attributes (parse-json-array request :image_attributes)
        new-images-attr (filter #(contains? % :checksum) image-attributes)
        existing-images-attr (remove #(contains? % :checksum) image-attributes)]
    {:images images :image-attributes image-attributes :new-images-attr new-images-attr :existing-images-attr existing-images-attr}))

;(defn prepare-image-attributes [tx images model-id validation-result new-images-attr existing-images-attr]
;  (let [created-images-attr (process-persist-images tx images model-id validation-result)
;        created-images-attr (update-image-attribute-ids new-images-attr created-images-attr)
;        all-image-attributes (into existing-images-attr created-images-attr)]
;    {:created-images-attr created-images-attr :all-image-attributes all-image-attributes}))

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

(defn patch-model-handler [req]
  (let [model-id (to-uuid (get-in req [:path-params :model_id]))
        pool-id (to-uuid (get-in req [:path-params :pool_id]))
        tx (:tx req)
        is-cover (-> req :body-params :is_cover)
        result (jdbc/execute! tx (-> (sql/update :models)
                                     (sql/set {:cover_image_id (to-uuid is-cover)})
                                     (sql/where [:= :id model-id])
                                     (sql/returning :id :cover_image_id)
                                     sql-format))]
    (response/response result)))

;(def CONST_FILE_PATH (str (System/getProperty "user.dir") "/tmp/"))
(def CONST_FILE_PATH "/tmp/")
(def CONST_FILE_PATH "/private/tmp/")
(def CONST_FILE_PATH "/Users/mradl/release/leihs/inventory")
;(def CONST_FILE_PATH "")

;/private/tmp/upload_thumb_thumb.png
(defn delete-image
  "Process:
            - Reset `cover_image_id` in the model if it matches the image ID.
            - Delete the image and its related entries."
  [req]
  (let [tx (:tx req)
        {:keys [model_id image_id]} (:path (:parameters req))
        id (to-uuid image_id)
        row (jdbc/execute-one! tx
                               (-> (sql/select :cover_image_id)
                                   (sql/from :models)
                                   (sql/where [:= :id model_id])
                                   sql-format))]

    (when (= (:cover_image_id row) id)
      (jdbc/execute! tx
                     (-> (sql/update :models)
                         (sql/set {:cover_image_id nil})
                         (sql/where [:= :id model_id])
                         sql-format)))

    (let [res (jdbc/execute-one! tx
                                 (sql-format
                                  {:with [[:ordered_images
                                           {:select [:id]
                                            :from [:images]
                                            :where [:or [:= :parent_id id] [:= :id id]]
                                            :order-by [[:parent_id :asc]]}]]
                                   :delete-from :images
                                   :where [:in :id {:select [:id] :from [:ordered_images]}]}))]
      (if (= (:next.jdbc/update-count res) 2)
        (response {:status "ok" :image_id image_id})
        (bad-request {:error "Failed to delete image"})))))










(defn resize-image
  "Resize an image using IM4Java."
  [input-path output-path width height]

  (let [cmd (ConvertCmd.)
        op (IMOperation.)]
    (.addImage op input-path)
    (.resize op width height)
    (.addImage op output-path)
    (.run cmd op)))


(defn resize-image
  "Resize an image using IM4Java."
  [input-path output-path width height]
   (println ">o> abc.in.out" input-path output-path)
  (let [cmd (ConvertCmd.)
        op (IMOperation.)]
    (.addImage op (into-array String [input-path]))
    (.resize op width height)
    (.addImage op (into-array String [output-path]))
    (.run cmd op)))


(defn resize-image
  "Resize an image using IM4Java."
  [input-path output-path width height]
  (let [cmd (ConvertCmd.)
        op (IMOperation.)]
    (.addImage op (into-array String [input-path]))
    ;; Ensure width and height are integers
    (.resize op (int width) (int height))
    (.addImage op (into-array String [output-path]))
    (.run cmd op)))



(defn resize-image
  "Resize an image using IM4Java."
  [input-path output-path width height]
  (let [cmd (ConvertCmd.)
        op (IMOperation.)]
    (.addImage op (into-array String [input-path]))
    (.resize op (int width) (int height))
    (.addImage op (into-array String [output-path]))
    (.run cmd op (into-array String []))))  ;; Corrected invocation

(defn get-file-size
  "Get the file size in bytes."
  [file-path]
  (.length (File. file-path)))

(defn file-to-base64
  "Convert a file to a Base64 string."
  [file-path]

 (println ">o> abc.to-base" file-path)
 (println ">o> abc.to-base >>>> " (:tempfile file-path))

  (with-open [input-stream (FileInputStream. (:tempfile file-path))  ;; Convert to string path
              baos (ByteArrayOutputStream.)]
    (let [buffer (byte-array 1024)]
      (loop []
        (let [bytes-read (.read input-stream buffer)]
          (when (pos? bytes-read)
            (.write baos buffer 0 bytes-read)
            (recur))))
      (let [encoder (Base64/getEncoder)]
        (.encodeToString encoder (.toByteArray baos))))))


(defn extract-file-path
  "Extracts the file path from a string or map with :tempfile key."
  [file-path]
  (cond
    (string? file-path) file-path
    (map? file-path) (get file-path :tempfile)
    :else nil))

(defn file-to-base64
  "Convert a file to a Base64 string."
  [file-path]
  (let [resolved-path (extract-file-path file-path)]

    (println ">o> abc.to-base: " resolved-path)
    (println ">o> abc.to-base >>>> " (if (map? file-path) (:tempfile file-path) "N/A"))

    (if (and resolved-path (.exists (File. resolved-path)))
      (with-open [input-stream (FileInputStream. resolved-path)
                  baos (ByteArrayOutputStream.)]
        (let [buffer (byte-array 1024)]
          (loop []
            (let [bytes-read (.read input-stream buffer)]
              (when (pos? bytes-read)
                (.write baos buffer 0 bytes-read)
                (recur))))
          (let [encoder (Base64/getEncoder)]
            (.encodeToString encoder (.toByteArray baos)))))
      (do
        (println "Error: Invalid file path or file does not exist:" resolved-path)
        nil))))

(defn resize-and-convert-to-base64
  "Resize the image, convert it to Base64, and get the file size."
  [input-path width height]
  (let [output-path (str CONST_FILE_PATH "resized_output.png")]
    (resize-image input-path output-path width height)
    (let [
          p (println ">o> abc.output-path" output-path)


          ;_     (Thread/sleep 2000)

          file-size (get-file-size output-path)

          p (println ">o> abc.file-size" file-size)

          base64-str (file-to-base64 output-path)

          p (println ">o> abc.base64-str" base64-str)

          ]
      ;; Clean up the temporary file
      ;(.delete (File. output-path))
      {:base64 base64-str
       :file-size file-size})))





(defn upload-image [req]
  (let [{{:keys [model_id]} :path} (:parameters req)
        body-stream (:body req)
        path CONST_FILE_PATH
        tx (:tx req)
        content-type (get-in req [:headers "content-type"])
        filename-to-save (sanitize-filename (get-in req [:headers "x-filename"]))
        content-length (some-> (get-in req [:headers "content-length"]) Long/parseLong)
        file-full-path (str path filename-to-save)

        p (println ">o> abc.file-full-path" file-full-path)

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

          ;file-full-path (str file-full-path ".thumb")


          main-image-data (add-thumb-to-filename main-image-data)
          ;p (println ">o> abc.main-image-data.after" main-image-data)
          ;main-image-data (add-thumb-to-filename main-image-data)



          ;file-content-thumb (generate-thumbnail file-content-main file-full-path (str "/tmp/" (:filename main-image-data)))
          ;file-content-thumb (generate-thumbnail  file-full-path (str CONST_FILE_PATH (:filename main-image-data)))

          p (println ">o> abc.file-full-path????" file-full-path)

          thumb-data (resize-and-convert-to-base64 file-full-path 100 100)

          ;base64-output (resize-and-convert-to-base64 input-path width height)
          ;thumb-data (resize-and-convert-to-base64 input-path width height)


      ;p (println ">o> abc.main-image-data.after-thumb-data" thumb-data)



          thumbnail-data (-> (assoc main-image-data :content (:base64 thumb-data) :size (:file-size thumb-data) :thumbnail true :parent_id (:id main-image-result))
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

(defn replace-nil-with-empty-string
  "Replace all nil values in a map with empty strings."
  [m]
  (into {}
        (for [[k v] m]
          [k (if (nil? v) "" v)])))

(defn rename-keys-in-vec
  [data key-map]
  (mapv #(clojure.set/rename-keys % key-map) data))

(defn extract-model-form-data-new [request create-all]
  (let [multipart (or (get-in request [:parameters :multipart])
                      (get-in request [:parameters :body]))
        prepared-model-data (-> (prepare-model-data multipart)
                                (assoc :is_package (str-to-bool (:is_package multipart))))
;; FIXME: CONVERT NIL-VALUES TO EMPTY-STR
        prepared-model-data (replace-nil-with-empty-string prepared-model-data)
        categories (-> multipart :categories)
        compatibles (-> multipart :compatibles)
        properties (-> multipart :properties)
        accessories (-> multipart :accessories)
        entitlements (rename-keys-in-vec (-> multipart :entitlements) {:group_id :entitlement_group_id})
        ;attachments (when create-all (normalize-files request :attachments)) ; maybe FIXME
        ;attachments-to-delete (-> multipart :attachments_to_delete)
        ;images-to-delete (-> multipart :images_to_delete)
        ;{:keys [images image-attributes new-images-attr existing-images-attr]}
        ;(when create-all (create-images-and-prepare-image-attributes request))
        ]
    {:prepared-model-data prepared-model-data
     :categories (if (nil? categories) [] categories)
     :compatibles compatibles
     :properties properties
     :accessories accessories
     :entitlements (if (nil? entitlements) [] entitlements)
     ;:attachments attachments
     ;:attachments-to-delete attachments-to-delete
     ;:images-to-delete images-to-delete
     ;:images images
     ;:new-images-attr new-images-attr
     ;:existing-images-attr existing-images-attr
     }))

(defn delete-where-clause [ids not-in-clause where-clause]
  (let [clean-ids (vec (filter some? ids))]
    (if (seq clean-ids)
      [:and [:not-in not-in-clause clean-ids] where-clause]
      where-clause)))

(defn extract-ids [entries]
  (vec (keep :id entries)))

(defn delete-entries [tx table id-key ids base-where]
  (let [where-clause (delete-where-clause ids id-key base-where)
        delete-query (-> (sql/delete-from table)
                         (sql/where where-clause)
                         (sql/returning :*)
                         sql-format)]
    (jdbc/execute! tx delete-query)))

(defn update-or-insert [tx table where-values update-values]
  (let [select-query (-> (sql/select :*)
                         (sql/from table)
                         (sql/where where-values)
                         sql-format)
        existing-entry (first (jdbc/execute! tx select-query))]
    (if existing-entry
      (jdbc/execute-one! tx (-> (sql/update table)
                                (sql/set update-values)
                                (sql/where where-values)
                                (sql/returning :*)
                                sql-format))
      (jdbc/execute-one! tx (-> (sql/insert-into table)
                                (sql/values [update-values])
                                (sql/returning :*)
                                sql-format)))))

(defn validate-empty-string!
  ([k vec-of-maps]
   (validate-empty-string! k vec-of-maps nil))
  ([k vec-of-maps scope]
   (doseq [m vec-of-maps]
     (when (and (contains? m k) (= "" (get m k)))
       (throw (ex-info (str "Field '" k "' cannot be an empty string.")
                       (merge {:key k :map m} (when scope {:scope scope}))))))))

(defn process-entitlements [tx entitlements model-id]
  (delete-entries tx :entitlements :id (extract-ids entitlements) [:= :model_id model-id])
  (doseq [entry entitlements]
    (let [id (to-uuid (:id entry))
          where-clause (if id
                         [:and [:= :id id] [:= :model_id model-id]]
                         [:and [:= :model_id model-id]
                          [:= :entitlement_group_id (to-uuid (:entitlement_group_id entry))]])]
      (update-or-insert tx :entitlements where-clause
                        {:model_id model-id
                         :entitlement_group_id (to-uuid (:entitlement_group_id entry))
                         :quantity (:quantity entry)}))))

(defn process-properties [tx properties model-id]
  (validate-empty-string! :key properties "properties")
  (delete-entries tx :properties :id (extract-ids properties) [:= :model_id model-id])
  (doseq [entry properties]
    (let [id (to-uuid (:id entry))
          where-clause (if id
                         [:and [:= :id id] [:= :model_id model-id]]
                         [:and [:= :model_id model-id] [:= :key (:key entry)]])]
      (update-or-insert tx :properties where-clause
                        {:model_id model-id
                         :key (:key entry)
                         :value (:value entry)}))))

(defn- update-accessory-pool-relation [tx accessory-id pool-id add?]
  (if add?
    (update-or-insert tx :accessories_inventory_pools
                      [:and [:= :accessory_id accessory-id] [:= :inventory_pool_id pool-id]]
                      {:accessory_id accessory-id :inventory_pool_id pool-id})
    (jdbc/execute! tx (-> (sql/delete-from :accessories_inventory_pools)
                          (sql/where [:= :accessory_id accessory-id] [:= :inventory_pool_id pool-id])
                          sql-format))))

(defn process-accessories [tx accessories model-id pool-id]
  (validate-empty-string! :name accessories "accessories")
  (delete-entries tx :accessories_inventory_pools :accessory_id (extract-ids accessories)
                  [:= :inventory_pool_id pool-id])
  (delete-entries tx :accessories :id (extract-ids accessories) [:= :model_id model-id])
  (doseq [entry accessories]
    (let [id (to-uuid (:id entry))
          where-clause (if id
                         [:= :id id]
                         [:and [:= :model_id model-id] [:= :name (:name entry)]])
          accessory (update-or-insert tx :accessories where-clause
                                      {:model_id model-id :name (:name entry)})
          accessory-id (:id accessory)]
      (update-accessory-pool-relation tx accessory-id pool-id (:inventory_bool entry)))))

(defn process-compatibles [tx compatibles model-id]
  (delete-entries tx :models_compatibles :compatible_id (extract-ids compatibles)
                  [:= :model_id model-id])
  (doseq [compatible compatibles]
    (let [compatible-id (to-uuid (:id compatible))]
      (update-or-insert tx :models_compatibles
                        [:and [:= :model_id model-id] [:= :compatible_id compatible-id]]
                        {:model_id model-id :compatible_id compatible-id}))))

(defn process-categories [tx categories model-id pool-id]
  (delete-entries tx :model_links :id (extract-ids categories) [:= :model_id model-id])
  (doseq [category categories]
    (let [category-id (to-uuid (:id category))]
      (update-or-insert tx :model_links
                        [:and [:= :model_id model-id] [:= :model_group_id category-id]]
                        {:model_id model-id :model_group_id category-id})
      (update-or-insert tx :inventory_pools_model_groups
                        [:and [:= :inventory_pool_id pool-id] [:= :model_group_id category-id]]
                        {:inventory_pool_id pool-id :model_group_id category-id}))))

(defn create-validation-response [data validation]
  {:data data
   :validation validation})

(defn filter-response [model keys]
  (apply dissoc model keys))
