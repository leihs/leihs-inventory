(ns leihs.inventory.server.resources.models.form.model.model-by-pool-form-update
  (:require
   [cheshire.core :as cjson]
   [clojure.data.codec.base64 :as b64]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.models.form.model.common :refer [create-images-and-prepare-image-attributes
                                                                      prepare-image-attributes]]
   [leihs.inventory.server.resources.models.helper :refer [str-to-bool normalize-model-data parse-json-array normalize-files
                                                           file-to-base64 base-filename process-attachments]]
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

(defn prepare-model-data
  [data]
  (let [normalize-data (normalize-model-data data)
        created-ts (LocalDateTime/now)
        res (assoc normalize-data :updated_at created-ts)]
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

(defn process-deletions [tx ids table key]
  (doseq [id ids]
    (jdbc/execute! tx (-> (sql/delete-from table)
                          (sql/where [:= key (to-uuid id)])
                          sql-format))))

(defn process-image-attributes "Process update/delete of images by image-attributes" [tx image-attributes model-id]
  (let [images-to-delete (map :id (filter :to_delete image-attributes))
        images-to-update (remove #(or (:to_delete %) (not (:is_cover %))) image-attributes)]
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
                                       :where [:in :id {:select [:id] :from [:ordered_images]}]}))))
    (doseq [{:keys [id is_cover]} images-to-update]
      (when is_cover
        (jdbc/execute! tx (-> (sql/update :models)
                              (sql/set {:cover_image_id (to-uuid id)})
                              (sql/where [:= :id model-id])
                              sql-format))))))

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

(defn filter-response [model keys]
  (let [updated-model (apply dissoc model keys)]
    updated-model))

(defn update-model-handler-by-pool-form [request]
  (let [validation-result (atom [])
        model-id (to-uuid (get-in request [:path-params :model_id]))
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
            updated-model (filter-response updated-model [:rental_price])
            compatibles (parse-json-array request :compatibles)
            categories (parse-json-array request :categories)
            attachments (normalize-files request :attachments)
            attachments-to-delete (parse-json-array request :attachments_to_delete)

            {:keys [images image-attributes new-images-attr existing-images-attr]}
            (create-images-and-prepare-image-attributes request)

            properties (parse-json-array request :properties)
            accessories (parse-json-array request :accessories)
            entitlements (parse-json-array request :entitlements)

            {:keys [created-images-attr all-image-attributes]}
            (prepare-image-attributes tx images model-id validation-result new-images-attr existing-images-attr)]

        (process-attachments tx attachments model-id)
        (process-deletions tx attachments-to-delete :attachments :id)
        (process-images tx images model-id)
        (process-image-attributes tx all-image-attributes model-id)
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
