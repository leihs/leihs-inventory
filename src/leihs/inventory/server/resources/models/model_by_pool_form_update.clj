(ns leihs.inventory.server.resources.models.model-by-pool-form-update
  (:require
   [clojure.data.codec.base64 :as b64]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.models.model-by-pool-form-fetch :refer [create-model-handler-by-pool-form-fetch]]
   [leihs.inventory.server.resources.models.queries :refer [accessories-query attachments-query base-pool-query
                                                            entitlements-query item-query
                                                            model-links-query properties-query]]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.helper :refer [convert-map-if-exist]]
   [next.jdbc :as jdbc]
   [pantomime.extract :as extract]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]])
  (:import [java.net URL JarURLConnection]
           (java.time LocalDateTime)
           [java.util UUID]))

(defn prepare-model-data
  [data]
  (let [key-map {:type :type
                 :manufacturer :manufacturer
                 :product :product
                 :version :version
                 :hand_over_note :importantNotes
                 :is_package :isPackage
                 :description :description
                 :internal_description :internalDescription
                 :technical_detail :technicalDetails}
        created-ts (LocalDateTime/now)
        renamed-data (reduce (fn [acc [db-key original-key]]
                               (if-let [val (get data original-key)]
                                 (assoc acc db-key val)
                                 acc))
                             {} key-map)]
    (assoc renamed-data :updated_at created-ts)))

(defn prepare-model-data
  [data]
  (let [p (println ">o> prepare-model-data.before" data)
        key-map {:type :type
                 :manufacturer :manufacturer
                 :product :product
                 :version :version
                 :hand_over_note :importantNotes
                 :is_package :isPackage
                 :description :description
                 :internal_description :internalDescription
                 :technical_detail :technicalDetails}
        now-ts (LocalDateTime/now)
        renamed-data (reduce (fn [acc [db-key original-key]]
                               (if-let [val (get data original-key)]
                                 (assoc acc db-key
                                        (if (= db-key :is_package)
                                          (boolean val) ;; Convert to boolean if key is :is_package
                                          val))
                                 acc))
                             {} key-map)]
    (assoc renamed-data :updated_at now-ts)))

(defn str-to-int [s]
  (try
    (Integer/parseInt s)
    (catch NumberFormatException e ;; catches invalid integer format
      (println ">o> str-to-int" e)
      nil)))

(defn str-to-bool
  [s]
  (cond
    (string? s) (case (.toLowerCase s)
                  "true" true
                  "false" false
                  nil) ; returns nil for any other string
    :else (boolean s))) ; for non-string inputs, just cast to boolean

(defn prepare-model-data
  [data]
  (let [p (println ">o> prepare-model-data.before" data)

        key-map {:type :type
                 :manufacturer :manufacturer
                 :product :product
                 :version :version
                 :hand_over_note :importantNotes
                 :is_package :isPackage
                 :description :description
                 :internal_description :internalDescription
                 :technical_detail :technicalDetails}
        created-ts (LocalDateTime/now)
        renamed-data (reduce (fn [acc [db-key original-key]]
                               ;(if-let [val (get data original-key)]
                               ;  (assoc acc db-key
                               ;    (if (= db-key :is_package)
                               ;      (boolean (some? val)) ;; Ensure it's true if value is truthy, else false
                               ;      val))

                               (if-let [val (get data original-key)]
                                 (assoc acc db-key val)
                                 acc))

;acc))
                             {} key-map)

        res (assoc renamed-data :updated_at created-ts)
        p (println ">o> prepare-model-data.after" res)
        p (println ">o> prepare-model-data.after2" (:is_package res) (type (:is_package res)))

        res (assoc res :is_package (str-to-bool (:is_package res)))

        p (println ">o> prepare-model-data.after2" res)]

    res))

(defn update-or-insert
  [tx table where-values update-values]
  (let [select-query (-> (sql/select :*)
                         (sql/from table)
                         (sql/where where-values)
                         sql-format)
        existing-entry (first (jdbc/execute! tx select-query))]
    (if existing-entry
      (let [update-query (-> (sql/update table)
                             (sql/set update-values)
                             (sql/where where-values)
                             (sql/returning :*)
                             sql-format)]
        (jdbc/execute-one! tx update-query))
      (let [insert-query (-> (sql/insert-into table)
                             (sql/values [update-values])
                             (sql/returning :*)
                             sql-format)]
        (jdbc/execute-one! tx insert-query)))))

(defn update-insert-or-delete
  [tx table where-values update-values entry]
  (if (:delete entry)
    (let [delete-query (-> (sql/delete-from table)
                           (sql/where where-values)
                           sql-format)]
      (jdbc/execute-one! tx delete-query))
    (let [select-query (-> (sql/select :*)
                           (sql/from table)
                           (sql/where where-values)
                           sql-format)
          existing-entry (first (jdbc/execute! tx select-query))]
      (if existing-entry
        (let [update-query (-> (sql/update table)
                               (sql/set update-values)
                               (sql/where where-values)
                               (sql/returning :*)
                               sql-format)]
          (jdbc/execute-one! tx update-query))
        (let [insert-query (-> (sql/insert-into table)
                               (sql/values [update-values])
                               (sql/returning :*)
                               sql-format)]
          (jdbc/execute-one! tx insert-query))))))

(defn parse-json-array
  [request key]
  (let [json-array-string (get-in request [:parameters :multipart key])]
    (cond
      (not json-array-string) []
      (and (string? json-array-string) (some #(= json-array-string %) ["" "[]"])) []
      :else (json/read-str json-array-string :key-fn keyword))))

(defn normalize-files
  [request key]
  (let [attachments (get-in request [:parameters :multipart key])
        normalized (if (map? attachments)
                     [attachments]
                     attachments)
        filtered (vec (filter #(pos? (:size % 0)) normalized))]
    filtered))

(defn base-filename
  [filename]
  (if-let [[_ base extension] (re-matches #"(.*)_thumb(\.[^.]+)$" filename)]
    (str base extension)
    filename))

(defn file-to-base64 [file]
  (let [actual-file (if (instance? java.io.File file)
                      file ; If `file` is already a java.io.File, use it directly
                      (:tempfile file))] ; Otherwise, extract :tempfile from the map

    (if actual-file
      (do
        ;(println "Debug: Using file object:" actual-file)  ; Print the actual file object

        ;; Read the file as bytes and encode to Base64
        (let [bytes (with-open [in (io/input-stream actual-file)
                                out (java.io.ByteArrayOutputStream.)]
                      (io/copy in out)
                      (.toByteArray out))]
          ;(println "Debug: File bytes:" bytes)  ; Print the raw byte array

          (let [encoded-str (String. (b64/encode bytes))]
            ;(println "Debug: Base64 encoded string:" encoded-str)  ; Print the Base64 string result
            encoded-str)))
      (do
        (println "Error: No valid file or tempfile found")
        nil))))

(defn update-model-handler-by-pool-form [request]
  (let [model-id (to-uuid (get-in request [:path-params :model_id]))
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        multipart (get-in request [:parameters :multipart])
        tx (:tx request)

        is-package (get-in request [:parameters :multipart :isPackage])
        p (println ">o> is-package.single" is-package)

        prepared-model-data (prepare-model-data multipart)
        p (println ">o> prepared-model-data" prepared-model-data)]
    (try
      (let [update-model-query (-> (sql/update :models)
                                   (sql/set prepared-model-data)
                                   (sql/where [:= :id model-id])
                                   (sql/returning :*)
                                   sql-format)
            updated-model (jdbc/execute-one! tx update-model-query)]

        (let [compatibles (parse-json-array request :compatibles)
              categories (parse-json-array request :categories)

              attachments (normalize-files request :attachments)
              attachments-to-delete (parse-json-array request :attachments-to-delete)
              images (normalize-files request :images)
              images-to-delete (parse-json-array request :images-to-delete)

              _ (println ">o> attachments-to-delete" attachments-to-delete)

              _ (println ">o> images-to-delete" images-to-delete)

              properties (parse-json-array request :properties)
              accessories (parse-json-array request :accessories)
              entitlements (parse-json-array request :entitlements)]

          (doseq [entry attachments]
            (let [file (:tempfile entry)
                  file-content (file-to-base64 file)
                  data (dissoc entry :tempfile)
                  data (assoc data :content file-content :model_id model-id)]
              (jdbc/execute! tx (-> (sql/insert-into :attachments)
                                    (sql/values [data])
                                    (sql/returning :*)
                                    sql-format))))

          (doseq [id attachments-to-delete]
            (println ">o> attachments-to-delete" attachments-to-delete)
            (jdbc/execute! tx
                           (-> (sql/delete-from :attachments)
                               (sql/where [:= :id (to-uuid id)])
                               sql-format)))

          (doseq [id images-to-delete]
            (println ">o> images-to-delete" id)

            (let [id (to-uuid id)

                  row (jdbc/execute-one! tx
                                         (-> (sql/select :*)
                                             (sql/from :models)
                                             (sql/where [:= :id model-id])
                                             sql-format))

                  _ (when (= (:cover_image_id row) id)
                      (jdbc/execute! tx
                                     (-> (sql/update :models)
                                         (sql/set {:cover_image_id nil})
                                         (sql/where [:= :id model-id])
                                         sql-format)))]

              (jdbc/execute! tx
                             (sql-format
                              {:with [[:ordered_images
                                       {:select [:id]
                                        :from [:images]
                                        :where [:or
                                                [:= :parent_id id]
                                                [:= :id id]]
                                        :order-by [[:parent_id :asc]]}]]
                               :delete-from :images
                               :where [:in :id {:select [:id] :from [:ordered_images]}]}))))

          (let [image-groups (group-by #(base-filename (:filename %)) images)
                CONST_ALLOW_IMAGE_WITH_THUMB_ONLY true]
            (doseq [[_ entries] image-groups]
              (when (and CONST_ALLOW_IMAGE_WITH_THUMB_ONLY (= 2 (count entries)))
                (let [[main-image thumb] (if (str/includes? (:filename (first entries)) "_thumb.")
                                           [(second entries) (first entries)]
                                           [(first entries) (second entries)])
                      target-id model-id
                      file (:tempfile main-image)
                      file-content (file-to-base64 main-image)
                      main-image-data (-> (set/rename-keys main-image {:content-type :content_type})
                                          (dissoc :tempfile)
                                          (assoc :content file-content
                                                 :target_id target-id
                                                 :target_type "Model"
                                                 :thumbnail false))
                      main-image-res (-> (sql/insert-into :images)
                                         (sql/values [main-image-data])
                                         (sql/returning :*)
                                         sql-format)
                      main-image-result (first (jdbc/execute! tx main-image-res))
                      file (:tempfile thumb)
                      file-content (file-to-base64 file)
                      thumbnail-data (-> (set/rename-keys thumb {:content-type :content_type})
                                         (dissoc :tempfile)
                                         (assoc :content file-content
                                                :target_id target-id
                                                :target_type "Model"
                                                :thumbnail true
                                                :parent_id (:id main-image-result)))]
                  (jdbc/execute! tx (-> (sql/insert-into :images)
                                        (sql/values [thumbnail-data])
                                        (sql/returning :*)
                                        sql-format))))))

          (doseq [entry entitlements]

            (println ">o> entitlements.entry >> " entry)
            (let [id (to-uuid (:entitlement_id entry))
                  where-clause (if (nil? id)
                                 [:and [:= :model_id model-id] [:= :entitlement_group_id (to-uuid (:entitlement_group_id entry))]]
                                 [:and [:= :id id] [:= :model_id model-id]])
                  ;quantity pa(:quantity entry)
                  ]
              (update-insert-or-delete tx
                                       :entitlements
                                       where-clause
                                       {:model_id model-id :entitlement_group_id (to-uuid (:entitlement_group_id entry)) :quantity (:quantity entry)} entry)))

          (doseq [entry properties]
            (let [id (to-uuid (:id entry))
                  where-clause (if (nil? id)
                                 [:and [:= :model_id model-id] [:= :key (:key entry)]]
                                 [:and [:= :id id] [:= :model_id model-id]])]
              (update-insert-or-delete tx
                                       :properties
                                       where-clause
                                       {:model_id model-id :key (:key entry) :value (:value entry)} entry)))

          (doseq [entry accessories]
            (if (:delete entry)
              (let [id (to-uuid (:id entry))]
                (jdbc/execute! tx (-> (sql/delete-from :accessories_inventory_pools)
                                      (sql/where [:= :accessory_id id] [:= :inventory_pool_id pool-id])
                                      sql-format))
                (jdbc/execute! tx (-> (sql/delete-from :accessories)
                                      (sql/where [:= :id id])
                                      sql-format)))
              (let [accessory-id (to-uuid (:id entry))
                    where-clause (if (nil? accessory-id)
                                   [:and [:= :model_id model-id] [:= :name (:name entry)]]
                                   [:= :id accessory-id])
                    accessory (update-or-insert tx
                                                :accessories
                                                where-clause
                                                {:model_id model-id :name (:name entry)})
                    accessory-id (:id accessory)]
                (if (:inventory_bool entry)

                  (update-or-insert tx
                                    :accessories_inventory_pools
                                    [:and [:= :accessory_id accessory-id] [:= :inventory_pool_id pool-id]]
                                    {:accessory_id accessory-id :inventory_pool_id pool-id})

                  (jdbc/execute! tx (-> (sql/delete-from :accessories_inventory_pools)
                                        (sql/where [:= :accessory_id accessory-id] [:= :inventory_pool_id pool-id])
                                        sql-format))))))

          (doseq [compatible compatibles]
            (let [compatible-id (to-uuid (:id compatible))
                  where-clause [:and [:= :model_id model-id] [:= :compatible_id compatible-id]]]
              (update-insert-or-delete tx
                                       :models_compatibles
                                       where-clause
                                       {:model_id model-id :compatible_id compatible-id}
                                       compatible)))

          (doseq [category categories]
            (let [category-id (to-uuid (:id category))]
              (if (:delete category)
                (jdbc/execute! tx (-> (sql/delete-from :model_links)
                                      (sql/where [:= :model_id model-id] [:= :model_group_id category-id])
                                      sql-format))
                (do
                  (update-or-insert tx
                                    :model_links
                                    [:and [:= :model_id model-id] [:= :model_group_id category-id]]
                                    {:model_id model-id :model_group_id category-id})
                  (update-or-insert tx
                                    :inventory_pools_model_groups
                                    [:and [:= :inventory_pool_id pool-id] [:= :model_group_id category-id]]
                                    {:inventory_pool_id pool-id :model_group_id category-id})))))

          (if updated-model
            (response [updated-model])
            (bad-request {:error "Failed to update model"}))))
      (catch Exception e
        (error "Failed to update model" (.getMessage e))
        (bad-request {:error "Failed to update model" :details (.getMessage e)})))))
