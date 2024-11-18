(ns leihs.inventory.server.resources.models.model-by-pool-form-create
  (:require
   [cheshire.core :as cjson]
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
        renamed-data (reduce (fn [acc [db-key original-key]]
                               (if-let [val (get data original-key)]
                                 (assoc acc db-key val)
                                 acc))
                             {}
                             key-map)]
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
                  nil)
    :else (boolean s)))

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

(defn base-filename
  [filename]
  (if-let [[_ base extension] (re-matches #"(.*)_thumb(\.[^.]+)$" filename)]
    (str base extension)
    filename))

(defn parse-json-array
  "Parse the JSON string and return the vector of maps. (swagger-ui normalizer)"
  [request key]
  (let [json-array-string (get-in request [:parameters :multipart key])
        p (println ">o> json-array-string" json-array-string (type json-array-string))]
    (cond
      (not json-array-string) []
      (and (string? json-array-string) (some #(= json-array-string %) ["" "[]" "{}"])) []

      :else (try
              (let [normalized-json-array-string
                    (if (and (.startsWith json-array-string "{")
                             (not (.startsWith json-array-string "[")))
                      (str "[" json-array-string "]")
                      json-array-string)

                    parsed (cjson/parse-string normalized-json-array-string true)
                    parsed-vector (vec parsed)]
                parsed-vector)
              (catch Exception e
                (throw (ex-info "Invalid JSON Array Format" {:error (.getMessage e)})))))))

(defn normalize-files
  [request key]
  (let [attachments (get-in request [:parameters :multipart key])
        normalized (if (map? attachments)
                     [attachments]
                     attachments)]
    (vec (filter #(pos? (:size % 0)) normalized))))

(defn file-to-base64 [file]
  (let [actual-file (if (instance? java.io.File file)
                      file
                      (:tempfile file))]
    (when actual-file
      (let [bytes (with-open [in (io/input-stream actual-file)
                              out (java.io.ByteArrayOutputStream.)]
                    (io/copy in out)
                    (.toByteArray out))]
        (String. (b64/encode bytes))))))

(defn create-validation-response [data validation]
  {:data data
   :validation validation})

(defn process-attachments [tx attachments model-id]
  (doseq [entry attachments]
    (let [file-content (file-to-base64 (:tempfile entry))
          data (assoc (dissoc entry :tempfile) :content file-content :model_id model-id)]
      (jdbc/execute! tx (-> (sql/insert-into :attachments)
                            (sql/values [data])
                            (sql/returning :*)
                            sql-format)))))

(defn process-images [tx images model-id validation-result]
  (let [image-groups (group-by #(base-filename (:filename %)) images)
        CONST_ALLOW_IMAGE_WITH_THUMB_ONLY true]
    (doseq [[_ entries] image-groups]
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
                                                          (sql/returning :*)
                                                          sql-format))
              file-content-thumb (file-to-base64 (:tempfile thumb))
              thumbnail-data (-> (set/rename-keys thumb {:content-type :content_type})
                                 (dissoc :tempfile)
                                 (assoc :content file-content-thumb
                                        :target_id model-id
                                        :target_type "Model"
                                        :thumbnail true
                                        :parent_id (:id main-image-result)))]
          (jdbc/execute! tx (-> (sql/insert-into :images)
                                (sql/values [thumbnail-data])
                                (sql/returning :*)
                                sql-format)))
        (swap! validation-result conj {:error "Either image or thumbnail is missing"
                                       :uploaded-file (:filename (first entries))})))))

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

(defn create-model-handler-by-pool-form [request]
  (let [validation-result (atom [])
        created-ts (LocalDateTime/now)
        tx (:tx request)
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        multipart (get-in request [:parameters :multipart])
        prepared-model-data (-> (prepare-model-data multipart)
                                (assoc :is_package (str-to-bool (:is_package multipart))))
        categories (parse-json-array request :categories)
        compatibles (parse-json-array request :compatibles)
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

        (process-attachments tx attachments model-id)
        (process-images tx images model-id validation-result)
        (process-entitlements tx entitlements model-id)
        (process-properties tx properties model-id)
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
