(ns leihs.inventory.server.resources.pool.models.model.packages.package.main
  (:require
   [cheshire.core :as json]
   [clojure.set]
   [clojure.string :as str]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.core :refer [presence]]
   [leihs.core.json :refer [to-json]]
   [leihs.inventory.server.resources.pool.common :refer [calculate-retired-value
                                                         parse-local-date-or-nil
                                                         double-to-numeric-or-nil
                                                         remove-nil-entries
                                                         remove-empty-entries
                                                         remove-empty-or-nil
                                                         remove-entries-by-keys
                                                         cast-to-uuid-or-nil
                                                         str-to-bool
                                                         parse-json-array
                                                         normalize-files
                                                         file-to-base64
                                                         base-filename
                                                         process-attachments]]
   [leihs.inventory.server.resources.pool.form-queries :refer [inventory-manager-package-subquery
                                                               lending-manager-package-subquery
                                                               package-base-query]]
   [leihs.inventory.server.resources.pool.models.helper :refer [fetch-latest-inventory-code
                                                                normalize-model-data
                                                                extract-shortname-and-number]]
   [leihs.inventory.server.resources.pool.models.model.packages.main :refer [prepare-package-data
                                                                             split-items]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-to-response]]
   [leihs.inventory.server.utils.helper :refer [url-ends-with-uuid? convert-map-if-exist]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response
                                                    fetch-pagination-params]]
   [leihs.inventory.server.utils.request-utils :refer [path-params
                                                       query-params]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [debug error]])
  (:import
   (java.time LocalDateTime)
   [java.time LocalDateTime]
   [java.time.format DateTimeFormatter]
   [java.util UUID]))

(defn get-current-timestamp []
  (let [current-timestamp (LocalDateTime/now)
        formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss")]
    (.format current-timestamp formatter)))

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

(defn get-resource [request]
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
          (response {:data model-result :fields fields})
          (status (response {:error "Failed to fetch item"
                             :details "No data found"}) 404)))
      (catch Exception e
        (error "Failed to fetch item" (.getMessage e))
        (bad-request {:error "Failed to fetch item" :details (.getMessage e)})))))

(defn update-package-handler [{item-id :item_id model-id :model_id pool-id :pool_id tx :tx request :request item-entry :item-entry}]
  (let [created-ts (LocalDateTime/now)
        tx (:tx request)
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        multipart (get-in request [:parameters :body])
        items_attributes (parse-json-array request :items_attributes)
        multipart (assoc multipart :inventory_pool_id pool-id)
        prepared-package-data (prepare-package-data multipart)
        split-items (split-items items_attributes)]
    (try
      (let [update-model-query (-> (sql/update [:items :i])
                                   (sql/set prepared-package-data)
                                   (sql/where [:and [:= :i.model_id model-id] [:= :i.id item-id]])
                                   (sql/returning :*)
                                   sql-format)
            res (jdbc/execute-one! tx update-model-query)

            ;; Link items from package
            link-res (let [ids-to-link (get split-items :ids-to-link)]
                       (when (seq ids-to-link)
                         (let [update-link-items-query (-> (sql/update :items)
                                                           (sql/set {:parent_id (:id res)})
                                                           (sql/where [:in :id ids-to-link])
                                                           (sql/where [:is :parent_id nil])
                                                           (sql/returning :*)
                                                           sql-format)
                               linked-items-res (jdbc/execute! tx update-link-items-query)]
                           linked-items-res)))

            ;; Unlink items from package
            unlink-res (let [ids-to-unlink (get split-items :ids-to-unlink)]
                         (when (seq ids-to-unlink)
                           (let [update-unlink-items-query (-> (sql/update :items)
                                                               (sql/set {:parent_id nil})
                                                               (sql/where [:in :id ids-to-unlink])
                                                               (sql/where [:is-not :parent_id nil])
                                                               (sql/returning :*)
                                                               sql-format)
                                 unlinked-items-res (jdbc/execute! tx update-unlink-items-query)]
                             unlinked-items-res)))]
        (if res
          (response res)
          (bad-request {:error "Failed to update item"})))
      (catch Exception e
        (error "Failed to update item" (.getMessage e))
        (bad-request {:error "Failed to update item" :details (.getMessage e)})))))

(defn fetch-package-data [tx model-id item-id pool-id]
  (let [query (-> (sql/select :*)
                  (sql/from :items)
                  (sql/where [:= :id item-id] [:= :model_id model-id] [:= :inventory_pool_id pool-id])
                  sql-format)
        res (jdbc/execute-one! tx query)]
    res))

(defn put-resource [request]
  (let [item-id (to-uuid (get-in request [:path-params :item_id]))
        model-id (to-uuid (get-in request [:path-params :model_id]))
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        tx (:tx request)
        res (fetch-package-data tx model-id item-id pool-id)]
    (if res
      (update-package-handler {:item_id item-id :model_id model-id :pool_id pool-id :tx tx :request request :item-entry res})
      (bad-request {:error "Failed to update item" :details "No data found"}))))
