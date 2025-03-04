(ns leihs.inventory.server.resources.models.form.package.model-by-pool-form-fetch
  (:require
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [honey.sql :as sq :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.core :refer [presence]]
   [leihs.inventory.server.resources.models.form.license.queries :refer [model-query
                                                                         inventory-manager-license-subquery
                                                                         lending-manager-license-subquery
                                                                         inventory-manager-item-subquery

                                                                         inventory-manager-package-subquery
                                                                         lending-manager-package-subquery

                                                                         item-base-query
                                                                         package-base-query
                                                                         lending-manager-item-subquery

                                                                         license-base-query]]
   [leihs.inventory.server.resources.models.helper :refer [fetch-latest-inventory-code]]
   [leihs.inventory.server.resources.models.queries :refer [accessories-query
                                                            attachments-query
                                                            entitlements-query
                                                            item-query

                                                            model-links-query
                                                            properties-query]]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.core :refer [single-entity-get-request?]]
   [leihs.inventory.server.utils.helper :refer [convert-map-if-exist]]
   [leihs.inventory.server.utils.pagination :refer [fetch-pagination-params
                                                    pagination-response
                                                    create-pagination-response]]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :as jdbco]
   [ring.util.response :as response :refer [bad-request]]
   [taoensso.timbre :refer [error]])
  (:import [java.time LocalDateTime]
           [java.time.format DateTimeFormatter]
           [java.util UUID]))

(defn get-current-timestamp []
  (let [current-timestamp (LocalDateTime/now)
        formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss")]
    (.format current-timestamp formatter)))

(defn build-select [fields]
  (mapv (fn [field] (:id field)) fields))

(defn filter-by-allowed-keys
  [data allowed-keys whitelisted-keys blacklisted-keys]
  (let [allowed-keywords (set (map keyword allowed-keys))
        whitelisted-keywords (set (map keyword whitelisted-keys))
        blacklisted-keywords (set (map keyword blacklisted-keys))
        all-keys (clojure.set/difference
                  (clojure.set/union allowed-keywords whitelisted-keywords)
                  blacklisted-keywords)]
    (reduce-kv
     (fn [result k v]
       (if (contains? all-keys k)
         (assoc result k v)
         result))
     {}
     data)))

(defn rename-keys
  "Renames keys in a map based on a provided key mapping.
   `key-map` is a map where the keys are old keys and the values are new keys."
  [m key-map]
  (reduce
   (fn [acc [old-key new-key]]
     (if (contains? m old-key)
       (assoc acc new-key (get m old-key))
       acc))
   (apply dissoc m (keys key-map))
   key-map))

(defn subquery-by-role [roles-for-pool]
  (let [roles (if (set? roles-for-pool)
                roles-for-pool
                (:roles roles-for-pool))
        subquery (cond
                   (contains? roles :inventory_manager) inventory-manager-package-subquery
                   (contains? roles :lending_manager) lending-manager-package-subquery
                   :else nil)]
    (when-not subquery
      (throw (Exception. "invalid role for the requested pool")))
    subquery))

(defn fetch-package-handler-by-pool-form [request]
  (let [current-timestamp (get-current-timestamp)
        tx (get-in request [:tx])
        roles-for-pool (:roles-for-pool request)
        item-id (to-uuid (get-in request [:path-params :item_id]))
        model-id (to-uuid (get-in request [:path-params :model_id]))
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        subquery (subquery-by-role roles-for-pool)]

    (try
      (let [query (-> (sql/select :*)
                      subquery
                      sql-format)

            fields (jdbc/execute! tx query)
            fields (conj fields {:active true
                                 :data {:type "autocomplete-search"
                                        :group "Inhalt"
                                        :label "Add Item"
                                        :values []}
                                 :attribute "quantity"
                                 :default false
                                 :forPackage true
                                 :group "Inhalt"
                                 :group_default "Inhalt"
                                 :id "add-item-group"
                                 :label "Add Item"})

            model-result (if model-id
                           ;; Fetch model data
                           (let [model-query (-> (package-base-query item-id model-id pool-id) sql-format)
                                 model-result (jdbc/execute-one! tx model-query)

                                 ;; remove all attr except defined keys
                                 model-result (filter-by-allowed-keys model-result
                                                                      ["product"
                                                                       "product_name"
                                                                       "model_id"
                                                                       "inventory_code"
                                                                       "inventory_pool_id"
                                                                       "responsible_department"
                                                                       "id"
                                                                       "building_id"
                                                                       "created_at"
                                                                       "updated_at"
                                                                       "owner_id"
                                                                       "retired"
                                                                       "retired_reason"
                                                                       "room_id"
                                                                       "shelf"
                                                                       "last_check"
                                                                       "is_borrowable"
                                                                       "is_inventory_relevant"
                                                                       "is_broken"
                                                                       "is_incomplete"
                                                                       "note"
                                                                       "status_note"
                                                                       "user_name"
                                                                       "price"]
                                                                      []
                                                                      [])

                                 items (jdbc/execute! tx
                                                      (-> (sql/select :i.id :i.inventory_code :i.serial_number :m.product :m.manufacturer)
                                                          (sql/from [:items :i])
                                                          (sql/join [:models :m] [:= :m.id :i.model_id])
                                                          (sql/where [:= :parent_id item-id])
                                                          sql-format))

                                 model-result (assoc model-result :items_attributes items)
                                 model-result (when model-result
                                                (let [model-result (assoc model-result
                                                                          :product {:name (:product_name model-result)
                                                                                    :model_id (:model_id model-result)})

                                                      model-result (rename-keys model-result {:item_version :version})
                                                      retired (not (nil? (:retired model-result)))
                                                      model-result (assoc model-result :retired retired)]
                                                  model-result))]
                             model-result)
                           ;; TODO: Fetch default, fixed version?
                           (let [responsible_department pool-id
                                 {:keys [next-code]} (fetch-latest-inventory-code tx nil)]
                             {:inventory_pool_id pool-id
                              :responsible_department responsible_department
                              :inventory_code next-code}))]

        (if model-result
          (response/response {:data model-result :fields fields})
          (response/status (response/response {:error "Failed to fetch item"
                                               :details "No data found"}) 404)))
      (catch Exception e
        (error "Failed to fetch item" (.getMessage e))
        (bad-request {:error "Failed to fetch item" :details (.getMessage e)})))))
