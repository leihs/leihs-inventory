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
   [leihs.inventory.server.resources.models.form.model.common :refer [prepare-image-attributes
                                                                      extract-model-form-data
                                                                      create-images-and-prepare-image-attributes]]
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
        normalize-data (dissoc normalize-data :id)]
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
(defn create-model-handler-by-pool-form [request create-all]
  (let [validation-result (atom [])
        created-ts (LocalDateTime/now)
        tx (:tx request)
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        {:keys [accessories prepared-model-data categories compatibles attachments properties
                entitlements images new-images-attr existing-images-attr]}
        (extract-model-form-data request create-all)]

    (try
      (let [res (jdbc/execute-one! tx (-> (sql/insert-into :models)
                                          (sql/values [prepared-model-data])
                                          (sql/returning :*)
                                          sql-format))
            res (filter-response res [:rental_price])
            model-id (:id res)
            {:keys [created-images-attr all-image-attributes]}
            (when create-all (prepare-image-attributes tx images model-id validation-result new-images-attr existing-images-attr))]
        (when create-all
          (process-attachments tx attachments "model_id" model-id)
          (process-image-attributes tx all-image-attributes model-id))
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

(defn create-model-handler-by-pool-model-only [request]
  (create-model-handler-by-pool-form request false))

(defn create-model-handler-by-pool-with-attachment-images [request]
  (create-model-handler-by-pool-form request true))
