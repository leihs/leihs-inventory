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
   [leihs.inventory.server.resources.models.form.common :refer [filter-keys db-operation]]
   [leihs.inventory.server.resources.models.form.model.common :refer [create-images-and-prepare-image-attributes
                                                                      extract-model-form-data
                                                                      prepare-image-attributes]]
   [leihs.inventory.server.resources.models.helper :refer [base-filename file-to-base64 normalize-files normalize-model-data
                                                           parse-json-array process-attachments str-to-bool]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
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

(defn process-delete-images-by-id "Process update/delete of images by image-attributes" [tx ids model-id]
  (let [images-to-delete ids]
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
                                       :where [:in :id {:select [:id] :from [:ordered_images]}]}))))))

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

(defn update-model-handler-by-pool-form [request create-all]
  (let [validation-result (atom [])
        model-id (to-uuid (get-in request [:path-params :model_id]))
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        tx (:tx request)
        {:keys [prepared-model-data categories compatibles attachments attachments-to-delete images-to-delete
                properties accessories entitlements images new-images-attr existing-images-attr]}
        (extract-model-form-data request create-all)]
    (try
      (let [update-model-query (-> (sql/update :models)
                                   (sql/set prepared-model-data)
                                   (sql/where [:= :id model-id])
                                   (sql/returning :*)
                                   sql-format)
            updated-model (jdbc/execute-one! tx update-model-query)
            updated-model (filter-response updated-model [:rental_price])
            {:keys [created-images-attr all-image-attributes]}
            (when create-all (prepare-image-attributes tx images model-id validation-result new-images-attr existing-images-attr))]

        (when create-all (process-attachments tx attachments model-id))
        (process-deletions tx attachments-to-delete :attachments :id)
        (when-not create-all (process-delete-images-by-id tx images-to-delete model-id))
        (when create-all (process-image-attributes tx all-image-attributes model-id))
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

(defn update-model-handler-by-pool-model-only [request]
  (update-model-handler-by-pool-form request false))

(defn update-model-handler-by-pool-with-attachment-images [request]
  (update-model-handler-by-pool-form request true))

(defn delete-model-handler-by-pool-form [request]
  (let [model-id (to-uuid (get-in request [:path-params :model_id]))
        tx (:tx request)
        models (db-operation tx :select :models [:= :id model-id])
        _ (when-not (seq models)
            (throw (ex-info "Request to delete model blocked: model not found" {:status 404})))

        items (db-operation tx :select :items [:= :model_id model-id])
        attachments (db-operation tx :select :attachments [:= :model_id model-id])
        images (db-operation tx :select :images [:= :target_id model-id])
        _ (when (seq items)
            (throw (ex-info "Request to delete model blocked: referenced item(s) exist" {:status 403})))

        deleted-model (jdbc/execute! tx
                                     (-> (sql/delete-from :models)
                                         (sql/where [:= :id model-id])
                                         (sql/returning :*)
                                         sql-format))
        _ (db-operation tx :delete :images [:= :target_id model-id])

        remaining-attachments (db-operation tx :select :attachments [:= :model_id model-id])
        remaining-images (db-operation tx :select :images [:= :target_id model-id])
        _ (when (or (seq remaining-attachments) (seq remaining-images))
            (throw (ex-info "Request to delete model blocked: referenced attachments or images still exist" {:status 403})))

        result {:deleted_attachments (filter-keys attachments [:id :model_id :filename :size])
                :deleted_images (filter-keys images [:id :target_id :filename :size :thumbnail])
                :deleted_model (filter-keys deleted-model [:id :product :manufacturer])}]

    (if (= 1 (count deleted-model))
      (response result)
      (throw (ex-info "Request to delete model failed" {:status 403})))))
