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
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.resources.pool.models.helper :refer [normalize-model-data ]]
   [leihs.inventory.server.utils.constants :refer [config-get]]
   [leihs.inventory.server.utils.image-upload-handler :refer [file-to-base64 resize-and-convert-to-base64]]
   [next.jdbc :as jdbc]
   [pantomime.extract :as extract]
   [ring.util.response :as response :refer [bad-request response status]]
   [taoensso.timbre :refer [error spy]])
  (:import [java.io File FileInputStream ByteArrayOutputStream]
           [java.net URL JarURLConnection]
           (java.time LocalDateTime)
           [java.util Base64]
           [java.util UUID]
           [java.util.jar JarFile]
           [org.im4java.core ConvertCmd IMOperation]))

;(defn create-image-url [col-name col-name-keyword]
;  [[[:raw (str "CASE WHEN " (name col-name) ".cover_image_id IS NOT NULL THEN CONCAT('/inventory/images/', " (name col-name) ".cover_image_id, '/thumbnail') ELSE NULL END")]]
;   col-name-keyword])

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

(defn delete-image
  [req]
  (let [tx (:tx req)
        {:keys [model_id image_id]} (:path (:parameters req))
        id (to-uuid image_id)]
    (let [res (jdbc/execute-one! tx
                                 (sql-format
                                  {:delete-from :images :where [:= :id id]}))]
      (if (= (:next.jdbc/update-count res) 1)
        (response {:status "ok" :image_id image_id})
        (bad-request {:error "Failed to delete image"})))))

(defn upload-image [req]
  (try
    (let [{{:keys [model_id]} :path} (:parameters req)
          body-stream (:body req)
          allowed-file-types (config-get :api :images :allowed-file-types)
          max-size-mb (config-get :api :images :max-size-mb)
          upload-path (config-get :api :upload-dir)
          tx (:tx req)
          content-type (get-in req [:headers "content-type"])
          filename-to-save (sanitize-filename (get-in req [:headers "x-filename"]))
          content-length (some-> (get-in req [:headers "content-length"]) Long/parseLong)
          file-full-path (str upload-path filename-to-save)
          entry {:tempfile file-full-path :filename filename-to-save :content_type content-type :size content-length :model_id model_id}]

      (let [allowed-extensions allowed-file-types
            content-extension (last (clojure.string/split content-type #"/"))]
        (when-not (some #(= content-extension %) allowed-extensions)
          (throw (ex-info "Invalid file type" {:status 400 :error "Unsupported file type"}))))

      (when (> content-length (* max-size-mb 1024 1024))
        (throw (ex-info "File size exceeds limit" {:status 400 :error "File size exceeds limit"})))

      (io/copy body-stream (io/file file-full-path))

      (let [file-content-main (file-to-base64 entry)
            main-image-data (-> entry
                                (assoc :content file-content-main :target_id model_id :target_type "Model" :thumbnail false)
                                filter-keys-images)
            main-image-result (jdbc/execute-one! tx (-> (sql/insert-into :images)
                                                        (sql/values [main-image-data])
                                                        image-response-format
                                                        sql-format))

            thumb-data (resize-and-convert-to-base64 file-full-path)

            thumbnail-data (-> (assoc main-image-data :content (:base64 thumb-data) :size (:file-size thumb-data) :thumbnail true :parent_id (:id main-image-result))
                               filter-keys-images)
            thumbnail-result (jdbc/execute-one! tx (-> (sql/insert-into :images)
                                                       (sql/values [thumbnail-data])
                                                       image-response-format
                                                       sql-format))
            data {:image main-image-result :thumbnail thumbnail-result :model_id model_id}]
        (status (response data) 200)))

    (catch Exception e
      (error "Failed to upload image" e)
      (bad-request {:error "Failed to upload image" :details (.getMessage e)}))))

(defn upload-attachment [req]
  (try
    (let [{{:keys [model_id]} :path} (:parameters req)
          body-stream (:body req)
          allowed-file-types (config-get :api :attachments :allowed-file-types)
          max-size-mb (config-get :api :attachments :max-size-mb)
          upload-path (config-get :api :upload-dir)
          tx (:tx req)
          content-type (get-in req [:headers "content-type"])
          filename-to-save (sanitize-filename (get-in req [:headers "x-filename"]))
          content-length (some-> (get-in req [:headers "content-length"]) Long/parseLong)
          file-full-path (str upload-path filename-to-save)
          entry {:tempfile file-full-path
                 :filename filename-to-save
                 :content_type content-type
                 :size content-length
                 :model_id model_id}]

      (let [allowed-extensions allowed-file-types
            content-extension (last (clojure.string/split content-type #"/"))]
        (when-not (some #(= content-extension %) allowed-extensions)
          (throw (ex-info "Invalid file type" {:status 400 :error "Unsupported file type"}))))

      (when (> content-length (* max-size-mb 1024 1024))
        (throw (ex-info "File size exceeds limit" {:status 400 :error "File size exceeds limit"})))

      (io/copy body-stream (io/file file-full-path))

      (let [file-content (file-to-base64 entry)
            data (-> entry
                     (assoc :content file-content)
                     filter-keys-attachments)
            data (jdbc/execute! tx (-> (sql/insert-into :attachments)
                                       (sql/values [data])
                                       attachment-response-format
                                       sql-format))]
        (status (response data) 200)))

    (catch Exception e
      (error "Failed to upload attachment" e)
      (bad-request {:error "Failed to upload attachment" :details (.getMessage e)}))))

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
