(ns leihs.inventory.server.resources.models.model-by-pool-form-create
  (:require
   [clojure.data.codec.base64 :as b64]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.models.queries :refer [accessories-query attachments-query base-pool-query
                                                            entitlements-query item-query
                                                            model-links-query properties-query]]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.core :refer [single-entity-get-request?]]
   [leihs.inventory.server.utils.helper :refer [convert-map-if-exist]]
   [leihs.inventory.server.utils.pagination :refer [fetch-pagination-params pagination-response create-pagination-response]]
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
  (let [created-ts (LocalDateTime/now)
        key-map {:type :type
                 :manufacturer :manufacturer
                 :product :product
                 :version :version
                 :hand_over_note :importantNotes
                 :is_package :isPackage
                 :description :description
                 :internal_description :internalDescription
                 :technical_detail :technicalDetails}
        renamed-data (->> key-map
                          (reduce (fn [acc [db-key original-key]]
                                    (if-let [val (get data original-key)]
                                      (assoc acc db-key val)
                                      acc))
                                  {}))]
    (assoc renamed-data
           :type "Model"
           :created_at created-ts
           :updated_at created-ts)))

(defn str-to-bool
  [s]
  (cond
    (string? s) (case (.toLowerCase s)
                  "true" true
                  "false" false
                  nil) ; returns nil for any other string
    :else (boolean s))) ; for non-string inputs, just cast to boolean

(defn create-or-use-existing
  [tx table where-values insert-values]
  (let [select-query (-> (sql/select :*)
                         (sql/from table)
                         (sql/where where-values)
                         sql-format)
        existing-entry (first (jdbc/execute! tx select-query))]
    (if existing-entry
      existing-entry
      (let [insert-query (-> (sql/insert-into table)
                             (sql/values [insert-values])
                             (sql/returning :*)
                             sql-format)
            new-entry (first (jdbc/execute! tx insert-query))]
        new-entry))))

(defn parse-uuid-values
  [key request]
  (let [raw-value (get-in request [:parameters :multipart key])
        p (println ">o> !!! raw-value " raw-value)
        ]
    (cond
      (instance? UUID raw-value) [raw-value]
      (and (instance? String raw-value) (not (str/includes? raw-value ","))) [(UUID/fromString raw-value)]
      (and (instance? String raw-value) (str/includes? raw-value ","))
      (mapv #(UUID/fromString %) (str/split raw-value #",\s*"))
      :else [])))

(defn base-filename
  [filename]
  (if-let [[_ base extension] (re-matches #"(.*)_thumb(\.[^.]+)$" filename)]
    (str base extension)
    filename))

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

(defn create-validation-response [data validation]
  {:data data
   :validation validation})

(defn valid-filetype?
  "Checks if all file entries have valid extensions.
   Allowed types are: jpg, jpeg, png."
  [entries]
  (let [allowed-types #{"jpg" "jpeg" "png"}]
    (every? #(allowed-types (clojure.string/lower-case (last (clojure.string/split % #"\.")))) entries)))

(defn create-model-handler-by-pool-form [request]
  (let [created_ts (LocalDateTime/now)
        model-id (get-in request [:path-params :model_id])
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        multipart (get-in request [:parameters :multipart])
        body-params (:body-params request)
        tx (:tx request)
        model (assoc body-params :created_at created_ts :updated_at created_ts)

        prepared-model-data (prepare-model-data multipart)
        prepared-model-data (assoc prepared-model-data :is_package (str-to-bool (:is_package prepared-model-data)))

        validation-result (atom []) ;; Use an atom to store validation results

        ;compatibles (parse-uuid-values :compatible_ids request)
        ;categories (parse-uuid-values :category_ids request)

        p (println ">o>  (keys multipart) >> " (keys multipart))

        compatibles (parse-json-array request :compatibles)
        p (println ">o> !!! compatibles " compatibles)

        categories (parse-json-array request :categories)
        p (println ">o> !!! categories " categories)



        attachments (normalize-files request :attachments)
        images (normalize-files request :images)
        properties (parse-json-array request :properties)
        accessories (parse-json-array request :accessories)
        entitlements (parse-json-array request :entitlements)]

    (try
      (let [res (jdbc/execute-one! tx (-> (sql/insert-into :models)
                                          (sql/values [prepared-model-data])
                                          (sql/returning :*)
                                          sql-format))
            model-id (:id res)]

        (doseq [entry attachments]
          (let [file (:tempfile entry)
                file-content (file-to-base64 file)
                data (dissoc entry :tempfile)
                data (assoc data :content file-content :model_id model-id)]
            (jdbc/execute! tx (-> (sql/insert-into :attachments)
                                  (sql/values [data])
                                  (sql/returning :*)
                                  sql-format))))

        (println ">o> images >>>" (count images) images)
        (let [image-groups (group-by #(base-filename (:filename %)) images)
              CONST_ALLOW_IMAGE_WITH_THUMB_ONLY true
              p (println ">o> ??? image-groups >>>" image-groups)]
          (doseq [[_ entries] image-groups]
            ;(when (and CONST_ALLOW_IMAGE_WITH_THUMB_ONLY (= 2 (count entries)))

            (println ">o> entries >> " entries)

            (if (and CONST_ALLOW_IMAGE_WITH_THUMB_ONLY (= 2 (count entries)))
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
                                      sql-format)))

              (swap! validation-result conj {:error "Either image or thumbnail is missing"
                                             :uploaded-file (:filename (first entries))}))))

        (doseq [entry entitlements]
          (create-or-use-existing tx
                                  :entitlements
                                  [:and
                                   [:= :model_id model-id]
                                   [:= :entitlement_group_id (to-uuid (:entitlement_group_id entry))]]
                                  {:model_id model-id :entitlement_group_id (to-uuid (:entitlement_group_id entry)) :quantity (:quantity entry)}))

        (doseq [entry properties]
          (create-or-use-existing tx
                                  :properties
                                  [:and
                                   [:= :model_id model-id]
                                   [:= :key (:key entry)]]
                                  {:model_id model-id :key (:key entry) :value (:value entry)}))

        (doseq [entry accessories]
          (let [accessory (create-or-use-existing tx
                                                  :accessories
                                                  [:and
                                                   [:= :model_id model-id]
                                                   [:= :name (:name entry)]]
                                                  {:model_id model-id :name (:name entry)})
                accessory-id (:id accessory)
                inv-pool-entry (if (:inventory_bool entry)
                                 (create-or-use-existing tx
                                                         :accessories_inventory_pools
                                                         [:and
                                                          [:= :accessory_id accessory-id]
                                                          [:= :inventory_pool_id pool-id]]
                                                         {:accessory_id accessory-id :inventory_pool_id pool-id})
                                 nil)]))

        ;(doseq [category-id compatibles]
        ;  (create-or-use-existing tx
        ;                          :models_compatibles
        ;                          [:and
        ;                           [:= :model_id model-id]
        ;                           [:= :compatible_id category-id]]
        ;                          {:model_id model-id :compatible_id category-id}))
        ;
        ;(doseq [category-id categories]
        ;  (create-or-use-existing tx
        ;                          :model_links
        ;                          [:and
        ;                           [:= :model_id model-id]
        ;                           [:= :model_group_id (to-uuid category-id)]]
        ;                          {:model_id model-id :model_group_id (to-uuid category-id)})
        ;  (create-or-use-existing tx
        ;                          :inventory_pools_model_groups
        ;                          [:and
        ;                           [:= :inventory_pool_id (to-uuid pool-id)]
        ;                           [:= :model_group_id (to-uuid category-id)]]
        ;                          {:inventory_pool_id (to-uuid pool-id) :model_group_id (to-uuid category-id)}))


          (println ">o> compatibles" compatibles)
        (doseq [compatible compatibles]
          (let [compatible-id (to-uuid (:id compatible))
                where-clause [:and [:= :model_id model-id] [:= :compatible_id compatible-id]]]
            (create-or-use-existing tx
              :models_compatibles
              where-clause
              {:model_id model-id :compatible_id compatible-id}
              )))

         (println ">o> categories" categories)
        (doseq [category categories]
          (let [category-id (to-uuid (:id category))]
            ;(if (:delete category)
            ;  (jdbc/execute! tx (-> (sql/delete-from :model_links)
            ;                      (sql/where [:= :model_id model-id] [:= :model_group_id category-id])
            ;                      sql-format))
              (do
                (create-or-use-existing tx
                  :model_links
                  [:and [:= :model_id model-id] [:= :model_group_id category-id]]
                  {:model_id model-id :model_group_id category-id})
                (create-or-use-existing tx
                  :inventory_pools_model_groups
                  [:and [:= :inventory_pool_id pool-id] [:= :model_group_id category-id]]
                  {:inventory_pool_id pool-id :model_group_id category-id}))))

        ;)


        (println ">o> >>> @validation-result" @validation-result)

        (if res
          ;(response [res])
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
