(ns leihs.inventory.server.resources.models.model-by-pool-form-update
  (:require
   [cheshire.core :as cjson]
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
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]])
  (:import [java.net URL JarURLConnection]
           (java.time LocalDateTime)
           [java.util UUID]))

(defn str-to-bool
  [s]
  (cond
    (string? s) (case (.toLowerCase s)
                  "true" true
                  "false" false
                  nil)
    :else (boolean s)))

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
                             {} key-map)
        res (assoc renamed-data :updated_at created-ts)]
    (assoc res :is_package (str-to-bool (:is_package res)))))

(defn update-or-insert
  [tx table where-values update-values]
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

(defn update-insert-or-delete
  [tx table where-values update-values entry]
  (if (:delete entry)
    (jdbc/execute-one! tx (-> (sql/delete-from table)
                              (sql/where where-values)
                              sql-format))
    (update-or-insert tx table where-values update-values)))

(defn parse-json-array
  "Parse the JSON string and return the vector of maps. (swagger-ui normalizer)"
  [request key]
  (let [json-array-string (get-in request [:parameters :multipart key])]
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
        normalized (if (map? attachments) [attachments] attachments)]
    (vec (filter #(pos? (:size % 0)) normalized))))

(defn base-filename
  [filename]
  (if-let [[_ base extension] (re-matches #"(.*)_thumb(\.[^.]+)$" filename)]
    (str base extension)
    filename))

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

(defn process-attachments [tx attachments model-id]
  (doseq [entry attachments]
    (let [file-content (file-to-base64 (:tempfile entry))
          data (assoc (dissoc entry :tempfile) :content file-content :model_id model-id)]
      (jdbc/execute! tx (-> (sql/insert-into :attachments)
                            (sql/values [data])
                            sql-format)))))

(defn process-deletions [tx ids table key]
  (doseq [id ids]
    (jdbc/execute! tx (-> (sql/delete-from table)
                          (sql/where [:= key (to-uuid id)])
                          sql-format))))

(defn process-image-deletions [tx images-to-delete model-id]
  (doseq [id images-to-delete]
    (let [id (to-uuid id)
          row (jdbc/execute-one! tx (-> (sql/select :*)
                                        (sql/from :models)
                                        (sql/where [:= :id model-id])
                                        sql-format))]
      (when (= (:cover_image_id row) id)
        (jdbc/execute! tx (-> (sql/update :models)
                              (sql/set {:cover_image_id nil})
                              (sql/where [:= :id model-id])
                              sql-format)))
      (jdbc/execute! tx (sql-format {:with [[:ordered_images
                                             {:select [:id]
                                              :from [:images]
                                              :where [:or [:= :parent_id id] [:= :id id]]
                                              :order-by [[:parent_id :asc]]}]]
                                     :delete-from :images
                                     :where [:in :id {:select [:id] :from [:ordered_images]}]})))))
(defn process-images [tx images model-id]
  (let [image-groups (group-by #(base-filename (:filename %)) images)]
    (doseq [[_ entries] image-groups]
      (when (= 2 (count entries))
        (let [[main-image thumb] (if (str/includes? (:filename (first entries)) "_thumb.")
                                   [(second entries) (first entries)]
                                   [(first entries) (second entries)])
              file-content (file-to-base64 (:tempfile main-image))
              main-image-data (-> (set/rename-keys main-image {:content-type :content_type})
                                  (dissoc :tempfile)
                                  (assoc :content file-content
                                         :target_id model-id
                                         :target_type "Model"
                                         :thumbnail false))
              main-image-result (first (jdbc/execute! tx (-> (sql/insert-into :images)
                                                             (sql/values [main-image-data])
                                                             (sql/returning :*)
                                                             sql-format)))
              file-content (file-to-base64 (:tempfile thumb))
              thumbnail-data (-> (set/rename-keys thumb {:content-type :content_type})
                                 (dissoc :tempfile)
                                 (assoc :content file-content
                                        :target_id model-id
                                        :target_type "Model"
                                        :thumbnail true
                                        :parent_id (:id main-image-result)))]
          (jdbc/execute! tx (-> (sql/insert-into :images)
                                (sql/values [thumbnail-data])
                                (sql/returning :*)
                                sql-format)))))))

(defn process-entitlements [tx entitlements model-id]
  (doseq [entry entitlements]
    (let [id (to-uuid (:entitlement_id entry))
          where-clause (if id
                         [:and [:= :id id] [:= :model_id model-id]]
                         [:and [:= :model_id model-id] [:= :entitlement_group_id (to-uuid (:entitlement_group_id entry))]])]
      (update-insert-or-delete tx :entitlements where-clause
                               {:model_id model-id
                                :entitlement_group_id (to-uuid (:entitlement_group_id entry))
                                :quantity (:quantity entry)} entry))))

(defn process-properties [tx properties model-id]
  (doseq [entry properties]
    (let [id (to-uuid (:id entry))
          where-clause (if id
                         [:and [:= :id id] [:= :model_id model-id]]
                         [:and [:= :model_id model-id] [:= :key (:key entry)]])]
      (update-insert-or-delete tx :properties where-clause
                               {:model_id model-id
                                :key (:key entry)
                                :value (:value entry)} entry))))

(defn process-accessories [tx accessories model-id pool-id]
  (doseq [entry accessories]
    (let [id (to-uuid (:id entry))]
      (if (:delete entry)
        (do
          (jdbc/execute! tx (-> (sql/delete-from :accessories_inventory_pools)
                                (sql/where [:= :accessory_id id] [:= :inventory_pool_id pool-id])
                                sql-format))
          (jdbc/execute! tx (-> (sql/delete-from :accessories)
                                (sql/where [:= :id id])
                                sql-format)))
        (let [where-clause (if id
                             [:= :id id]
                             [:and [:= :model_id model-id] [:= :name (:name entry)]])
              accessory (update-or-insert tx :accessories where-clause
                                          {:model_id model-id :name (:name entry)})
              accessory-id (:id accessory)]
          (if (:inventory_bool entry)
            (update-or-insert tx :accessories_inventory_pools
                              [:and [:= :accessory_id accessory-id] [:= :inventory_pool_id pool-id]]
                              {:accessory_id accessory-id :inventory_pool_id pool-id})
            (jdbc/execute! tx (-> (sql/delete-from :accessories_inventory_pools)
                                  (sql/where [:= :accessory_id accessory-id] [:= :inventory_pool_id pool-id])
                                  sql-format))))))))

(defn process-compatibles [tx compatibles model-id]
  (doseq [compatible compatibles]
    (let [compatible-id (to-uuid (:id compatible))]
      (update-insert-or-delete tx :models_compatibles
                               [:and [:= :model_id model-id] [:= :compatible_id compatible-id]]
                               {:model_id model-id :compatible_id compatible-id}
                               compatible))))

(defn process-categories [tx categories model-id pool-id]
  (doseq [category categories]
    (let [category-id (to-uuid (:id category))]
      (if (:delete category)
        (jdbc/execute! tx (-> (sql/delete-from :model_links)
                              (sql/where [:= :model_id model-id] [:= :model_group_id category-id])
                              sql-format))
        (do
          (update-or-insert tx :model_links
                            [:and [:= :model_id model-id] [:= :model_group_id category-id]]
                            {:model_id model-id :model_group_id category-id})
          (update-or-insert tx :inventory_pools_model_groups
                            [:and [:= :inventory_pool_id pool-id] [:= :model_group_id category-id]]
                            {:inventory_pool_id pool-id :model_group_id category-id}))))))

(defn update-model-handler-by-pool-form [request]
  (let [model-id (to-uuid (get-in request [:path-params :model_id]))
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        multipart (get-in request [:parameters :multipart])
        tx (:tx request)
        prepared-model-data (prepare-model-data multipart)]

    (try
      (let [update-model-query (-> (sql/update :models)
                                   (sql/set prepared-model-data)
                                   (sql/where [:= :id model-id])
                                   (sql/returning :*)
                                   sql-format)
            updated-model (jdbc/execute-one! tx update-model-query)
            compatibles (parse-json-array request :compatibles)
            categories (parse-json-array request :categories)
            attachments (normalize-files request :attachments)
            attachments-to-delete (parse-json-array request :attachments-to-delete)
            images (normalize-files request :images)
            images-to-delete (parse-json-array request :images-to-delete)
            properties (parse-json-array request :properties)
            accessories (parse-json-array request :accessories)
            entitlements (parse-json-array request :entitlements)]

        (process-attachments tx attachments model-id)
        (process-deletions tx attachments-to-delete :attachments :id)
        (process-images tx images model-id)
        (process-image-deletions tx images-to-delete model-id)
        (process-entitlements tx entitlements model-id)
        (process-properties tx properties model-id)
        (process-accessories tx accessories model-id pool-id)
        (process-compatibles tx compatibles model-id)
        (process-categories tx categories model-id pool-id)

        (if updated-model
          (response [updated-model])
          (bad-request {:error "Failed to update model"})))
      (catch Exception e
        (error "Failed to update model" (.getMessage e))
        (bad-request {:error "Failed to update model" :details (.getMessage e)})))))
