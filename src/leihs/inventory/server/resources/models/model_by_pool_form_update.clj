(ns leihs.inventory.server.resources.models.model-by-pool-form-update
  (:require
   [clojure.data.codec.base64 :as b64]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.pprint :refer [pprint]]
   [clojure.set :as set]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.models.queries :refer [accessories-query attachments-query base-pool-query
                                                            entitlements-query item-query
                                                            model-links-query properties-query]]

   [leihs.inventory.server.resources.models.model-by-pool-form-fetch :refer [ create-model-handler-by-pool-form-fetch]]

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

(defn parse-uuid-values
  "Parses UUIDs from a comma-separated string or single string. Returns a vector of UUIDs."
  [key request]
  (let [raw-value (get-in request [:parameters :multipart key])]
    (cond
      (instance? UUID raw-value) [raw-value]
      (and (instance? String raw-value) (not (str/includes? raw-value ","))) [(UUID/fromString raw-value)]
      (and (instance? String raw-value) (str/includes? raw-value ","))
      (mapv #(UUID/fromString %) (str/split raw-value #",\s*"))
      :else [])))

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

(defn normalize-files
  [request key]
  (let [attachments (get-in request [:parameters :multipart key])
        normalized (if (map? attachments)
                     [attachments]
                     ;(vector attachments)
                     attachments)

        _ (println ">o> before normalize-files" normalized (type normalized))
        ;; Filter out entries with :size 0
        filtered  (vec (filter #(pos? (:size % 0)) normalized))

        ]

    ;; Print the filtered files for debugging
    (println ">o> normalize-files" filtered (type filtered))

    ;; Return the filtered files
    filtered))

(defn parse-json-array
  [request key]
  (let [json-array-string (get-in request [:parameters :multipart key])

        p (println ">o> json-array-string??" json-array-string (type json-array-string) (string? json-array-string))
        ]
    (cond
      (not json-array-string) []
      (and (string? json-array-string) (some #(= json-array-string %) ["" "[]"])) []
      ;:else (json/read-str (str "[" json-array-string "]") :key-fn keyword))
      :else (json/read-str  json-array-string :key-fn keyword))
    ))

(defn update-model-handler-by-pool-form [request]
  (let [model-id (to-uuid(get-in request [:path-params :model_id]))
        pool-id (to-uuid (get-in request [:path-params :pool_id]))

        p (println ">o> abc" model-id (type model-id) pool-id (type pool-id))

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

        ;; Update associated tables with parsed values
        (let [compatibles (parse-uuid-values :compatible_ids request)
              categories (parse-uuid-values :category_ids request)
              attachments (normalize-files request :attachments)
              images (normalize-files request :images)
              properties (parse-json-array request :properties)
              accessories (parse-json-array request :accessories)
              entitlements (parse-json-array request :entitlements)]


           (println ">o> abX.1" )

          ;; Update entitlements
          (doseq [entry entitlements]
            (update-or-insert tx
              :entitlements
              [:and [:= :model_id model-id] [:= :entitlement_group_id (to-uuid (:entitlement_group_id entry))]]
              {:model_id model-id :entitlement_group_id (to-uuid (:entitlement_group_id entry)) :quantity (:quantity entry)}))

           (println ">o> abX.2" )
          ;; Update properties
          (doseq [entry properties]
            (update-or-insert tx
              :properties
              [:and [:= :model_id model-id] [:= :key (:key entry)]]
              {:model_id model-id :key (:key entry) :value (:value entry)}))

           (println ">o> abX.3" )
          ;; Update accessories
          (doseq [entry accessories]
            (let [accessory (update-or-insert tx
                              :accessories
                              [:and [:= :model_id model-id] [:= :name (:name entry)]]
                              {:model_id model-id :name (:name entry)})
                  accessory-id (:id accessory)]
              (when (:inventory_bool entry)
                (update-or-insert tx
                  :accessories_inventory_pools
                  [:and [:= :accessory_id accessory-id] [:= :inventory_pool_id pool-id]]
                  {:accessory_id accessory-id :inventory_pool_id pool-id}))))

           (println ">o> abX.4" )
          ;; Update compatible models
          (doseq [compatible-id compatibles]
            (update-or-insert tx
              :models_compatibles
              [:and [:= :model_id model-id] [:= :compatible_id compatible-id]]
              {:model_id model-id :compatible_id compatible-id}))

           (println ">o> abX.5" )
          ;; Update model categories
          (doseq [category-id categories]
            (update-or-insert tx
              :model_links
              [:and [:= :model_id model-id] [:= :model_group_id category-id]]
              {:model_id model-id :model_group_id category-id})
            (update-or-insert tx
              :inventory_pools_model_groups
              [:and [:= :inventory_pool_id pool-id] [:= :model_group_id category-id]]
              {:inventory_pool_id pool-id :model_group_id category-id}))

           (println ">o> abX.6" )
          ;; Additional file handling for attachments and images (similar to existing logic)

          (if updated-model
            (response [updated-model])

            ;(create-model-handler-by-pool-form-fetch request)

            (bad-request {:error "Failed to update model"}))))
      (catch Exception e
        (error "Failed to update model" (.getMessage e))
        (bad-request {:error "Failed to update model" :details (.getMessage e)})))))