(ns leihs.inventory.server.resources.models.model-by-pool-form-update
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
   [leihs.inventory.server.resources.models.model-by-pool-form-fetch :refer [create-model-handler-by-pool-form-fetch]]
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
            updated-model (jdbc/execute-one! tx update-model-query)]

        (let [compatibles (parse-json-array request :compatibles)
              categories (parse-json-array request :categories)
              attachments (normalize-files request :attachments)
              images (normalize-files request :images)
              properties (parse-json-array request :properties)
              accessories (parse-json-array request :accessories)
              entitlements (parse-json-array request :entitlements)]

          (doseq [entry entitlements]
            (let [id (to-uuid (:entitlement_id entry))
                  where-clause (if (nil? id)
                                 [:and [:= :model_id model-id] [:= :entitlement_group_id (to-uuid (:entitlement_group_id entry))]]
                                 [:and [:= :id id] [:= :model_id model-id]])]
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
                (when (:inventory_bool entry)
                  (update-or-insert tx
                    :accessories_inventory_pools
                    [:and [:= :accessory_id accessory-id] [:= :inventory_pool_id pool-id]]
                    {:accessory_id accessory-id :inventory_pool_id pool-id})))))

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
