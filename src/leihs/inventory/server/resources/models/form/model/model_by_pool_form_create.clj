(ns leihs.inventory.server.resources.models.form.model.model-by-pool-form-create
  (:require
   [cheshire.core :as cjson]
   [clojure.data.codec.base64 :as b64]
   [clojure.data.json :as json]
   [clojure.java.io :as io]

   [clojure.set :as set]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.models.form.model.model-by-pool-form-update :refer [filter-response process-image-attributes]]


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

(defn prepare-model-data
  [data]
  (let [normalize-data (normalize-model-data data)
        created-ts (LocalDateTime/now)

        normalize-data (dissoc normalize-data :id)
        ]
    (assoc normalize-data
      :type "Model"
      :created_at created-ts
      :updated_at created-ts)))

(defn create-or-use-existing
  [tx table where-values insert-values]
  (let [select-query (-> (sql/select :*)
                       (sql/from table)
                       (sql/where where-values)
                       sql-format)
        existing-entry (first (jdbc/execute! tx select-query))]
    (if existing-entry
      existing-entry
      (jdbc/execute-one! tx (-> (sql/insert-into table)
                              (sql/values [insert-values])
                              (sql/returning :*)
                              sql-format)))))

(defn create-validation-response [data validation]
  {:data data
   :validation validation})

(defn process-images [tx images model-id validation-result]
  (let [image-groups (group-by #(base-filename (:filename %)) images)
        CONST_ALLOW_IMAGE_WITH_THUMB_ONLY false]
    (doseq [[_ entries] image-groups]
      (println ">o> abc.entries:" entries)
      (if (and CONST_ALLOW_IMAGE_WITH_THUMB_ONLY (= 2 (count entries)))
        (let [[main-image thumb] (if (str/includes? (:filename (first entries)) "_thumb.")
                                   [(second entries) (first entries)]
                                   [(first entries) (second entries)])
              file-content-main (file-to-base64 (:tempfile main-image))
              main-image-data (-> (set/rename-keys main-image {:content-type :content_type})
                                (dissoc :tempfile)
                                (assoc :content file-content-main
                                  :target_id model-id
                                  :target_type "Model"
                                  :thumbnail false))
              main-image-result (jdbc/execute-one! tx (-> (sql/insert-into :images)
                                                        (sql/values [main-image-data])
                                                        ;(sql/returning :*)
                                                        (sql/returning :id :filename :thumbnail)
                                                        sql-format))
              p (println ">o> abc.img" main-image-result)

              file-content-thumb (file-to-base64 (:tempfile thumb))
              thumbnail-data (-> (set/rename-keys thumb {:content-type :content_type})
                               (dissoc :tempfile)
                               (assoc :content file-content-thumb
                                 :target_id model-id
                                 :target_type "Model"
                                 :thumbnail true
                                 :parent_id (:id main-image-result)))

              thumbnail-result (jdbc/execute! tx (-> (sql/insert-into :images)
                                                   (sql/values [thumbnail-data])
                                                   ;(sql/returning :*)
                                                   (sql/returning :id :filename :thumbnail)
                                                   sql-format))


              p (println ">o> abc.thumb" thumbnail-result)
              ]


          ;; TODO: how to gather results and return it
          {:general entries
           :image main-image-result
           :image_thumbnail thumbnail-result
           }

          )
        (swap! validation-result conj {:error "Either image or thumbnail is missing"
                                       :uploaded-file (:filename (first entries))})))))



(defn process-images [tx images model-id validation-result]
  (let [image-groups (group-by #(base-filename (:filename %)) images)
        CONST_ALLOW_IMAGE_WITH_THUMB_ONLY false]

    (reduce
      (fn [acc [_ entries]]
        (println ">o> abc.entries:" entries)

        (if (and CONST_ALLOW_IMAGE_WITH_THUMB_ONLY (= 2 (count entries)))
          (let [[main-image thumb] (if (str/includes? (:filename (first entries)) "_thumb.")
                                     [(second entries) (first entries)]
                                     [(first entries) (second entries)])
                file-content-main (file-to-base64 (:tempfile main-image))
                main-image-data (-> (set/rename-keys main-image {:content-type :content_type})
                                  (dissoc :tempfile)
                                  (assoc :content file-content-main
                                    :target_id model-id
                                    :target_type "Model"
                                    :thumbnail false))
                main-image-result (jdbc/execute-one! tx (-> (sql/insert-into :images)
                                                          (sql/values [main-image-data])
                                                          (sql/returning :id :filename :thumbnail)
                                                          sql-format))]

            (println ">o> abc.img" main-image-result)

            (let [file-content-thumb (file-to-base64 (:tempfile thumb))
                  thumbnail-data (-> (set/rename-keys thumb {:content-type :content_type})
                                   (dissoc :tempfile)
                                   (assoc :content file-content-thumb
                                     :target_id model-id
                                     :target_type "Model"
                                     :thumbnail true
                                     :parent_id (:id main-image-result)))
                  thumbnail-result (jdbc/execute! tx (-> (sql/insert-into :images)
                                                       (sql/values [thumbnail-data])
                                                       (sql/returning :id :filename :thumbnail)
                                                       sql-format))]

              (println ">o> abc.thumb" thumbnail-result)

              ;; Accumulate results
              (conj acc {:general entries
                         :image main-image-result
                         :image_thumbnail thumbnail-result})))

          ;; If validation fails, log an error
          (do
            (swap! validation-result conj {:error "Either image or thumbnail is missing"
                                           :uploaded-file (:filename (first entries))})
            acc)))
      []
      image-groups)))

(defn generate-thumbnail [a] a)


(defn add-thumb-to-filename [image-map]
  (update image-map :filename
    (fn [filename]
      (let [[name ext] (clojure.string/split filename #"\.(?=[^.]+$)")] ;; Split on last dot
        (str name "_thumb." ext)))))

(defn get-uploaded-file-size [tempfile]
  (.length tempfile))


(defn update-image-attribute-ids [new-images-attr created-images]
  (vec (map (fn [image]
         (let [matching-entry (some #(when (= (:checksum image)
                                             (str (:filename %) ":" (:size %)))
                                       %)
                                created-images)]
           (if matching-entry
             (assoc image :id (:id matching-entry))
             image)))
    new-images-attr)))


(defn process-persist-images [tx images model-id validation-result]
  (reduce
    (fn [acc image]
      (println ">o> Processing image:" (:filename image))

      ;; Convert main image to base64
      (let [

            tempfile (:tempfile image)
            filesize (get-uploaded-file-size tempfile)
            p (println ">o> abc.tmp-file.size?" filesize)

            file-content-main (file-to-base64 tempfile)
            main-image-data (-> (set/rename-keys image {:content-type :content_type})
                              (dissoc :tempfile)
                              (assoc :content file-content-main
                                :target_id model-id
                                :target_type "Model"
                                :thumbnail false))

            ;; Insert main image into the database
            main-image-result (jdbc/execute-one! tx (-> (sql/insert-into :images)
                                                      (sql/values [main-image-data])
                                                      (sql/returning :id :filename :thumbnail :size)
                                                      sql-format))
            p (println ">o> main-image-data:" (dissoc main-image-data :content))
            p (println ">o> main-image-result:" main-image-result)

            ;image-result (assoc main-image-result :size (:size main-image-data))

            p (println ">o> main-image-result.ab:" main-image-result)


        ;    ]
        ;
        ;
        ;;; Generate thumbnail content from the main image
        ;(let [


              file-content-thumb (generate-thumbnail file-content-main) ;; Function to create thumbnail from image content

              p (println ">o> abc1")
              ;p (println ">o> abc1.file-content-thumb" file-content-thumb)
              p (println ">o> abc1.main-image-result" main-image-result)

              ;thumbnail-data (-> main-image-data
              ;                 ;add-thumb-to-filename
              ;                 (assoc :content file-content-thumb
              ;                   :thumbnail
              ;                   :parent_id (:id main-image-result)))

            main-image-data (add-thumb-to-filename main-image-data)
              thumbnail-data (assoc main-image-data
                               :content file-content-thumb
                               ;:filename (add-thumb-to-filename main-image-data)
                               :thumbnail true              ; <-- Assign an actual value
                               :parent_id (:id main-image-result))


              p (println ">o> abc1")

              p (println ">o> thumbnail-data1:" (dissoc thumbnail-data :content))

              p (println ">o> abc2")

              ;; Insert thumbnail into the database
              thumbnail-result (jdbc/execute-one! tx (-> (sql/insert-into :images)
                                                       (sql/values [thumbnail-data])
                                                       (sql/returning :id :filename :thumbnail :size)
                                                       sql-format))
              p (println ">o> abc3")

            ;  p (println ">o> thumbnail-data2:" (dissoc thumbnail-data :content))
            ;
            ;thumb-result (assoc thumbnail-result :size (:size thumbnail-result))

            p (println ">o> thumb-result-result.thumbnail-result:" thumbnail-result)

              ]

          ;(println ">o> Inserted thumbnail:" thumbnail-result)
          ;
          ;;; Accumulate results
          ;(conj acc {:image main-image-result
          ;           :thumbnail thumbnail-result})
          ;)

        ;(conj acc {:image main-image-result
        ;           ;:thumbnail thumbnail-result
        ;           })
        ;
        ;
        ;(into (or acc []) [{:image main-image-result
        ;                    :thumbnail thumbnail-result}])

        (into (or acc []) [main-image-result thumbnail-result])



        ))
    []
    images))


(defn process-entitlements [tx entitlements model-id]
  (doseq [entry entitlements]
    (create-or-use-existing tx
      :entitlements
      [:and
       [:= :model_id model-id]
       [:= :entitlement_group_id (to-uuid (:entitlement_group_id entry))]]
      {:model_id model-id
       :entitlement_group_id (to-uuid (:entitlement_group_id entry))
       :quantity (:quantity entry)})))

(defn process-properties [tx properties model-id]
  (doseq [entry properties]
    (create-or-use-existing tx
      :properties
      [:and
       [:= :model_id model-id]
       [:= :key (:key entry)]]
      {:model_id model-id
       :key (:key entry)
       :value (:value entry)})))

(defn process-accessories [tx accessories model-id pool-id]
  (doseq [entry accessories]
    (let [accessory (create-or-use-existing tx
                      :accessories
                      [:and
                       [:= :model_id model-id]
                       [:= :name (:name entry)]]
                      {:model_id model-id :name (:name entry)})
          accessory-id (:id accessory)]
      (when (:inventory_bool entry)
        (create-or-use-existing tx
          :accessories_inventory_pools
          [:and
           [:= :accessory_id accessory-id]
           [:= :inventory_pool_id pool-id]]
          {:accessory_id accessory-id
           :inventory_pool_id pool-id})))))

(defn process-compatibles [tx compatibles model-id]
  (doseq [compatible compatibles]
    (create-or-use-existing tx
      :models_compatibles
      [:and
       [:= :model_id model-id]
       [:= :compatible_id (to-uuid (:id compatible))]]
      {:model_id model-id
       :compatible_id (to-uuid (:id compatible))})))

(defn process-categories [tx categories model-id pool-id]
  (doseq [category categories]
    (create-or-use-existing tx
      :model_links
      [:and
       [:= :model_id model-id]
       [:= :model_group_id (to-uuid (:id category))]]
      {:model_id model-id
       :model_group_id (to-uuid (:id category))})
    (create-or-use-existing tx
      :inventory_pools_model_groups
      [:and
       [:= :inventory_pool_id pool-id]
       [:= :model_group_id (to-uuid (:id category))]]
      {:inventory_pool_id pool-id
       :model_group_id (to-uuid (:id category))})))


(defn sort-images-by-attributes [images image-attributes]
  (let [attr-map (into {} (map (fn [{:keys [checksum] :as attr}]
                                 (let [[_ filename size] (clojure.string/split checksum #":")]
                                   [(str filename ":" size) attr]))
                            image-attributes))]
    (sort-by (fn [image]
               (let [key (str (:filename image) ":" (:size image))
                     attr (attr-map key)]
                 [(or (:is_cover attr) false) ;; Sort by is_cover first (true first)
                  (:size image)              ;; Then by size (ascending)
                  (:filename image)]))       ;; Then by filename (ascending)
      images)))


(defn sort-images-by-attributes [images image-attributes]
  (let [attr-map (into {} (map (fn [{:keys [checksum] :as attr}]
                                 (let [[_ filename size] (clojure.string/split checksum #":")]
                                   [(str filename ":" size) attr]))
                            image-attributes))]
    ;; Ensure all images have a corresponding checksum entry
    (doseq [image images]
      (let [key (str (:filename image) ":" (:size image))]
        (when-not (contains? attr-map key)
          (throw (Exception. (str "Missing checksum for image: " key))))))

    ;; Sort images based on attributes
    (sort-by (fn [image]
               (let [key (str (:filename image) ":" (:size image))
                     attr (attr-map key)]
                 [(or (:is_cover attr) false) ;; Sort by is_cover first (true first)
                  (:size image)              ;; Then by size (ascending)
                  (:filename image)]))       ;; Then by filename (ascending)
      (vec images))))


(defn update-image-attributes [image-attributes created-images]
  (let [id-map (into {} (map (fn [{:keys [image]}]
                               [(:filename image) (:id image)])
                          created-images))]
    (map (fn [attr]
           (let [[_ filename size] (clojure.string/split (:checksum attr) #":")]
             (if-let [new-id (id-map filename)]
               (assoc attr :id new-id)  ;; Replace id with the new one
               (throw (Exception. (str "No matching image found for: " filename))))))
      image-attributes)))

(defn create-model-handler-by-pool-form [request]
  (let [validation-result (atom [])
        p (println ">o> abc.create-model-handler-by-pool-form")
        created-ts (LocalDateTime/now)
        tx (:tx request)
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        multipart (get-in request [:parameters :multipart])
        prepared-model-data (-> (prepare-model-data multipart)
                              (assoc :is_package (str-to-bool (:is_package multipart))))
        categories (parse-json-array request :categories)
        compatibles (parse-json-array request :compatibles)
        attachments (normalize-files request :attachments)
        properties (parse-json-array request :properties)

        ;; Process persisting of images and updating id
        images (normalize-files request :images)

        ;(file-sha256 file)
        _ (doseq [file images]
          (println ">o> !!!!!! SHA-256:" (file-sha256 file)))

        image-attributes (parse-json-array request :image_attributes)
        new-images-attr (vec (filter #(contains? % :checksum)          image-attributes))
        existing-images-attr (vec (filter #(not (contains? % :checksum))          image-attributes))

        p (println ">o> ?? abc.new-images-attr" (count new-images-attr) new-images-attr)
        p (println ">o> ?? abc.existing-images-attr" (count existing-images-attr) existing-images-attr)

        p (println ">o> abc.images" images)
        p (println ">o> abc.image-attributes" image-attributes)

        accessories (parse-json-array request :accessories)
        entitlements (parse-json-array request :entitlements)]

    (try
      (let [

            res (jdbc/execute-one! tx (-> (sql/insert-into :models)
                                        (sql/values [prepared-model-data])
                                        (sql/returning :*)
                                        sql-format))
            res (filter-response res [:rental_price])
            model-id (:id res)

            ;; Process persisting of images and updating id
            created-images-attr (process-persist-images tx images model-id validation-result)
            created-images-attr (update-image-attribute-ids new-images-attr created-images-attr)
            p (println ">o> ?? abc.created-images-attr" (count created-images-attr) created-images-attr)


            all-image-attributes (into existing-images-attr created-images-attr)
            p (println ">o> ?? abc.all-image-attributes" (count all-image-attributes) all-image-attributes)

            ;image-attributes (update-image-attributes image-attributes created-images)

            ;p (println ">o> ?? abc.created-images" created-images)
            ;p (println ">o> abc.created-images.image.size" (count created-images))
            ]

        (process-attachments tx attachments "model_id" model-id)
        (process-entitlements tx entitlements model-id)
        (process-properties tx properties model-id)

        (process-image-attributes tx all-image-attributes model-id)

        (process-accessories tx accessories model-id pool-id)
        (process-compatibles tx compatibles model-id)
        (process-categories tx categories model-id pool-id)

        (if res
          (response (create-validation-response res @validation-result))
          (bad-request {:error "Failed to create model"})))
      (catch Exception e
        (error "Failed to create model" (.getMessage e))
        (cond
          (str/includes? (.getMessage e) "unique_model_name_idx")
          (-> (response {:status "failure"
                         :message "Model already exists"
                         :detail {:product (:product prepared-model-data)}})
            (status 409))
          (str/includes? (.getMessage e) "insert or update on table \"models_compatibles\"")
          (-> (response {:status "failure"
                         :message "Modification of models_compatibles failed"
                         :detail {:product (:product prepared-model-data)}})
            (status 409))
          :else (bad-request {:error "Failed to create model" :details (.getMessage e)}))))))
