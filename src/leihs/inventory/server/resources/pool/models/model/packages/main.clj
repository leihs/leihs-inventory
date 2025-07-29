(ns leihs.inventory.server.resources.pool.models.model.packages.main
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

(defn index-resources [request]
  (let [current-timestamp (get-current-timestamp)
        tx (get-in request [:tx])
        roles-for-pool (:roles-for-pool request)
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        item-id nil
        model-id nil
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

(def defaults
  {:is_borrowable false
   :is_broken false
   :is_incomplete false
   :is_inventory_relevant true

   :properties {:reference "invoice" :installation_status "inStorage"}})

(defn set-missing-defaults [data defaults]
  (reduce (fn [acc [k v]]
            (if (contains? acc k)
              acc
              (assoc acc k v)))
          data
          defaults))

(defn prepare-package-data [data]
  (let [created-ts (LocalDateTime/now)
        db-retired nil
        data (set-missing-defaults data defaults)
        request-retired (:retired data)
        data (if (= false request-retired)
               (assoc data :retired_reason nil)
               data)

        retired-value (calculate-retired-value db-retired request-retired)
        data (assoc data :retired retired-value)
        invoice-date (parse-local-date-or-nil (:invoice_date data))
        price (double-to-numeric-or-nil (:price data))
        data (assoc data :updated_at created-ts :last_check created-ts
                    :created_at created-ts :invoice_date invoice-date :price price)

        data (remove-nil-entries data [:invoice_date :price :room_id :last_check :user_name :shelf :status_note :note])
        data (remove-empty-entries data [:room_id :last_check :user_name :shelf :status_note :note])

        data (dissoc data :items_attributes)
        data (convert-map-if-exist data)]
    data))

(defn split-items
  "Splits items into two vectors:
   - `ids-to-unlink` (items with `:delete true`)
   - `ids-to-link` (items without `:delete` or `false`)."
  [items]
  (reduce
   (fn [{:keys [ids-to-unlink ids-to-link]} item]
     (if (:delete item)
       {:ids-to-unlink (conj ids-to-unlink (to-uuid (:id item)))
        :ids-to-link ids-to-link}
       {:ids-to-unlink ids-to-unlink
        :ids-to-link (conj ids-to-link (to-uuid (:id item)))}))
   {:ids-to-unlink [] :ids-to-link []}
   items))

(defn generate-or-verify-inventory-code! [tx data pool-id]
  (let [inv-code (:inventory_code data)
        next-code (:next-code (fetch-latest-inventory-code tx pool-id))]
    (when (and inv-code (not= next-code inv-code))
      (throw (ex-info "The inventory code is invalid or outdated" {:status 400})))
    (if inv-code
      data
      (assoc data :inventory_code next-code))))

(defn post-resource [request]
  (let [created-ts (LocalDateTime/now)
        tx (:tx request)
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        data (get-in request [:parameters :body])
        items_attributes (parse-json-array request :items_attributes)
        data (assoc data :inventory_pool_id pool-id)
        ;;TODO: is this logic correct?
        data (if (nil? (:owner_id data))
               (do
                 (println ">> ToCHECK / WARNING: no owner_id set, default: pool_id=" pool-id)
                 (assoc data :owner_id pool-id))
               data)

        data (generate-or-verify-inventory-code! tx data pool-id)
        prepared-package-data (prepare-package-data data)]

    (try
      (let [res (jdbc/execute-one! tx (-> (sql/insert-into :items)
                                          (sql/values [prepared-package-data])
                                          (sql/returning :*)
                                          sql-format))

            split-items (split-items items_attributes)

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
                             unlinked-items-res)))
            item-id (:id res)]

        (if res
          (response res)
          (bad-request {:error "Failed to create model"})))
      (catch Exception e
        (error "Failed to create model" (.getMessage e))
        (cond
          (str/includes? (.getMessage e) "unique_model_name_idx")
          (-> (response {:status "failure"
                         :message "Model already exists"
                         :detail {:product (:product prepared-package-data)}})
              (status 409))
          (str/includes? (.getMessage e) "insert or update on table \"models_compatibles\"")
          (-> (response {:status "failure"
                         :message "Modification of models_compatibles failed"
                         :detail {:product (:product prepared-package-data)}})
              (status 409))
          :else (bad-request {:error "Failed to create model" :details (.getMessage e)}))))))
