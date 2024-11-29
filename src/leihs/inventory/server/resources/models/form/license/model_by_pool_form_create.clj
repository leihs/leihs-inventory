(ns leihs.inventory.server.resources.models.form.license.model-by-pool-form-create
  (:require
   [cheshire.core :as cjson]
   [clojure.data.codec.base64 :as b64]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [cheshire.core :as jsonc]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.models.helper :refer [str-to-bool normalize-model-data parse-json-array normalize-files normalize-license-data
                                                           file-to-base64 base-filename process-attachments]]
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
  (let [normalize-data (normalize-license-data data)
        created-ts (LocalDateTime/now)]
    (assoc normalize-data
           ;:type "Software"
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

(defn extract-properties [multipart]
  (let [properties-entries (filter (fn [[k _]]
                                     (and (string? k)
                                       (.startsWith (name k) "properties,")))
                             multipart)
        properties-map (into {} (map (fn [[k v]]
                                       [(keyword (last (clojure.string/split (name k) #","))) v])
                                  properties-entries))]
    {:properties properties-map}))

(defn extract-properties [multipart]
  (let [properties-entries (filter (fn [[k _]]
                                     (and (keyword? k) ; Check if key is a keyword
                                       (.startsWith (name k) "properties,"))) ; Check if key name starts with "properties,"
                             multipart)
        properties-map (into {} (map (fn [[k v]]
                                       [(keyword (subs (name k) (count "properties,"))) v]) ; Extract the part after "properties,"
                                  properties-entries))]
    {:properties properties-map}))

(defn remove-empty-or-nil
  "Removes all entries from the map where the value is either nil or an empty string."
  [m]
  (into {}
    (filter (fn [[_ v]] (not (or (nil? v) (= v ""))))
      m)))
(defn create-license-handler-by-pool-form [request]
  (let [validation-result (atom [])
        now-ts (LocalDateTime/now)
        tx (:tx request)
        p (println ">o> abc1")
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        model-id (to-uuid (get-in request [:path-params :model_id]))

        multipart (get-in request [:parameters :multipart])
        p (println ">o> multipart1" multipart)


        properties (first (parse-json-array request :properties))
        p (println ">o> properties" properties)


        ;; TODO:
        ;; 1. set attachments
        ;; 2. default-room

        p (println ">o> abc.retired" (:retired multipart) (type (:retired multipart)))

        ;multipart2 (dissoc multipart  :attachments :retired :invoice_date :price)
        multipart2 (dissoc multipart :attachments :retired )
        ;multipart2 (dissoc multipart :attachments :properties :retired :invoice_date :price)
        multipart2b {
                     :created_at now-ts
                     :updated_at now-ts

                     :retired (if (= (:retired multipart) false)
                                nil
                                (.toLocalDate now-ts)
                                )

                     :properties [:cast (jsonc/generate-string properties) :jsonb]

                     ;:owner_id (to-uuid "8bd16d45-056d-5590-bc7f-12849f034351")
                     :inventory_pool_id pool-id

                     :model_id model-id

                     :room_id (to-uuid "503870e1-7fe5-44ef-89e7-11f1c40a9e70")
                     }

        multipart2 (merge multipart2 multipart2b)

        multipart2 (remove-empty-or-nil multipart2)
        p (println ">o> multipart2.multipart2 ???" multipart2)



        extracted-properties (extract-properties multipart)
        p (println ">o> multipart2.extract-properties" extracted-properties)


        p (println ">o> pool-id" pool-id)

        model-data (-> (prepare-model-data multipart)
                                (assoc :is_package (str-to-bool (:is_package multipart))))
        p (println ">o> !!! abc2.model-data" model-data)
        attachments (normalize-files request :attachments)
        p (println ">o> abc3.attachments" attachments)

        properties (first (parse-json-array request :properties))





        p (println ">o> !!! abc4.properties" properties)
        p (println ">o> !!! abc4.model-data" model-data)
        ]

    (try
      (let [

            p (println ">o> multipart2" multipart2)

            ;; FIXME: This is a hack to get the model data
            res (jdbc/execute-one! tx (-> (sql/insert-into :items)
                                          ;(sql/values [model-data])
                                          (sql/values [multipart2])
                                          (sql/returning :*)
                                          sql-format))
            model-id (:id res)

            ;res {:foo "bar"}
            ]

        ;(process-attachments tx attachments model-id)
        ;(process-images tx images model-id validation-result)
        ;(process-entitlements tx entitlements model-id)
        ;(process-properties tx properties model-id)
        ;(process-accessories tx accessories model-id pool-id)
        ;(process-compatibles tx compatibles model-id)
        ;(process-categories tx categories model-id pool-id)

        (if res
          (response (create-validation-response res @validation-result))
          (bad-request {:error "Failed to create model"})))
      (catch Exception e
        (error "Failed to create model" (.getMessage e))
        (cond
          (str/includes? (.getMessage e) "unique_model_name_idx")
          (-> (response {:status "failure"
                         :message "Model already exists"
                         :detail {:product (:product model-data)}})
              (status 409))
          (str/includes? (.getMessage e) "insert or update on table \"models_compatibles\"")
          (-> (response {:status "failure"
                         :message "Modification of models_compatibles failed"
                         :detail {:product (:product model-data)}})
              (status 409))
          :else (bad-request {:error "Failed to create model" :details (.getMessage e)}))))))
