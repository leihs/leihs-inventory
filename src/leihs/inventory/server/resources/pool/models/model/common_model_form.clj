(ns leihs.inventory.server.resources.pool.models.model.common-model-form
  (:require
   [cheshire.core :as cjson]
   [clojure.data.codec.base64 :as b64]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.java.shell :refer [sh]]
   [clojure.set :as set]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.common :refer [str-to-bool]]
   [leihs.inventory.server.resources.pool.models.helper :refer [normalize-model-data]]
   [leihs.inventory.server.utils.constants :refer [config-get]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.image-upload-handler :refer [file-to-base64 resize-and-convert-to-base64]]
   [next.jdbc :as jdbc]
   [pantomime.extract :as extract]
   [ring.util.response :as response :refer [bad-request response status]]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error spy]])
  (:import [java.io File FileInputStream ByteArrayOutputStream]
           [java.net URL JarURLConnection]
           (java.time LocalDateTime)
           [java.util Base64]
           [java.util UUID]
           [java.util.jar JarFile]
           [org.im4java.core ConvertCmd IMOperation]))

(defn patch-resource [req]
  (let [model-id (to-uuid (get-in req [:path-params :model_id]))
        pool-id (to-uuid (get-in req [:path-params :pool_id]))
        tx (:tx req)
        is-cover (-> req :body-params :is_cover)
        image (jdbc/execute-one! tx (-> (sql/select :*)
                                        (sql/from :images)
                                        (sql/where [:= :id (to-uuid is-cover)])
                                        sql-format))]
    (if (nil? image)
      (bad-request {:error "Image not found"})
      (response/response (jdbc/execute-one! tx (-> (sql/update :models)
                                                   (sql/set {:cover_image_id (to-uuid is-cover)})
                                                   (sql/where [:= :id model-id])
                                                   (sql/returning :id :cover_image_id)
                                                   sql-format))))))

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

(defn extract-model-form-data [request]
  (let [multipart (or (get-in request [:parameters :multipart])
                      (get-in request [:parameters :body]))
        prepared-model-data (-> (prepare-model-data multipart)
                                (assoc :is_package (str-to-bool (:is_package multipart))))
        prepared-model-data (replace-nil-with-empty-string prepared-model-data)
        categories (-> multipart :categories)
        compatibles (-> multipart :compatibles)
        properties (-> multipart :properties)
        accessories (-> multipart :accessories)
        entitlements (rename-keys-in-vec (-> multipart :entitlements) {:group_id :entitlement_group_id})]
    {:prepared-model-data prepared-model-data
     :categories (if (nil? categories) [] categories)
     :compatibles compatibles
     :properties properties
     :accessories accessories
     :entitlements (if (nil? entitlements) [] entitlements)}))

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
