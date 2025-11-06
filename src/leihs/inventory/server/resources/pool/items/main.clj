(ns leihs.inventory.server.resources.pool.items.main
  (:require
   [clojure.set :as set]
   [clojure.string :as string]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.constants :refer [PROPERTIES_PREFIX]]
   [leihs.inventory.server.resources.pool.fields.main :as fields]
   [leihs.inventory.server.resources.pool.items.shared :as items-shared]
   [leihs.inventory.server.resources.pool.items.types :as types]
   [leihs.inventory.server.resources.pool.models.common :refer [fetch-thumbnails-for-ids]]
   [leihs.inventory.server.utils.authorize.main :refer [authorized-role-for-pool]]
   [leihs.inventory.server.utils.coercion.core :refer [instant-to-date-string]]
   [leihs.inventory.server.utils.debug :refer [log-by-severity]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [leihs.inventory.server.utils.pick-fields :refer [pick-fields]]
   [leihs.inventory.server.utils.request-utils :refer [body-params path-params
                                                       query-params]]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response]]
   [taoensso.timbre :refer [debug]]))

(def ERROR_GET_ITEMS "Failed to get items")

(def item-columns
  [:items.id
   :items.model_id
   :items.name
   :items.inventory_pool_id

   :insurance_number
   :inventory_code
   :invoice_date
   :invoice_number
   :is_borrowable
   :is_broken
   :is_incomplete
   :is_inventory_relevant
   :item_version
   :last_check
   :needs_permission
   :note
   :owner_id
   :parent_id
   :price
   :properties
   :responsible
   :retired
   :retired_reason
   :room_id
   :serial_number
   :shelf
   :status_note
   :supplier_id
   :user_name
   :m.cover_image_id])

(defn index-resources
  ([request]
   (let [tx (:tx request)
         {:keys [pool_id]} (path-params request)
         {:keys [fields search_term
                 model_id parent_id
                 retired borrowable
                 incomplete broken owned
                 inventory_pool_id
                 in_stock before_last_check]} (query-params request)

         select (apply sql/select
                       (concat item-columns
                               [[:ip.name :inventory_pool_name]
                                [:r.end_date :reservation_end_date]
                                [:r.user_id :reservation_user_id]
                                [:r.contract_id :reservation_contract_id]
                                [:m.is_package :is_package]
                                [:m.name :model_name]
                                [:rs.name :room_name]
                                [:rs.description :room_description]
                                [:b.name :building_name]
                                [:b.code :building_code]

                                [[:nullif [:concat_ws " " :u.firstname :u.lastname] ""] :reservation_user_name]

                                [(-> (sql/select :%count.*) ; [[:count :*]]
                                     (sql/from [:items :i])
                                     (sql/where [:= :i.parent_id :items.id]))
                                 :package_items]]))

         query (-> select
                   (sql/from :items)
                   ;; Join inventory pool
                   (sql/join [:inventory_pools :ip]
                             [:= :ip.id :items.inventory_pool_id])

                   ;; Join rooms
                   (sql/join [:rooms :rs]
                             [:= :items.room_id :rs.id])

                   ;; Join rooms
                   (sql/join [:buildings :b]
                             [:= :rs.building_id :b.id])

                   ;; Join models
                   (sql/left-join [:models :m]
                                  [:or
                                   [:= :items.model_id :m.id]
                                   [:= :m.id model_id]])

                   ;; Join reservations (only active)
                   (sql/left-join [:reservations :r]
                                  [:and
                                   [:= :r.item_id :items.id]
                                   [:= :r.returned_date nil]])

                    ;; Join users
                   (sql/left-join [:users :u]
                                  [:= :u.id :r.user_id])

                    ;; Filters
                   (sql/where [:or
                               [:= :items.inventory_pool_id pool_id]
                               [:= :items.owner_id pool_id]])

                   (cond-> model_id (sql/where [:= :items.model_id model_id]))
                   (cond-> parent_id (sql/where [:= :items.parent_id parent_id]))

                   (items-shared/item-query-params pool_id inventory_pool_id
                                                   owned in_stock before_last_check
                                                   retired borrowable broken incomplete)

                   (cond-> (seq search_term)
                     (sql/where [:or
                                 [:ilike :items.inventory_code (str "%" search_term "%")]
                                 [:ilike :m.product (str "%" search_term "%")]
                                 [:ilike :m.manufacturer (str "%" search_term "%")]])))

         post-fnc (fn [items]
                    ;; Prepare items for thumbnail fetching by using model_id as id
                    (let [items-for-fetch (map (fn [item]
                                                 (assoc item :id (:model_id item)))
                                               items)
                          ;; Fetch thumbnails using model data
                          items-with-images (fetch-thumbnails-for-ids tx items-for-fetch)]
                      ;; Merge back with original items and add image URLs
                      (map-indexed (fn [idx item-with-img]
                                     (let [original-item (nth items idx)]
                                       (cond-> original-item
                                         (:image_id item-with-img)
                                         (assoc :image_id (:image_id item-with-img)
                                                :url (str "/inventory/" pool_id "/models/" (:model_id original-item) "/images/" (:image_id item-with-img))
                                                :content_type (:content_type item-with-img)))))
                                   items-with-images)))]

     (debug (sql-format query :inline true))
     (try
       (-> request
           (create-pagination-response query nil post-fnc)
           (pick-fields fields types/index-item)
           response)
       (catch Exception e
         (log-by-severity ERROR_GET_ITEMS e)
         (exception-handler request ERROR_GET_ITEMS e))))))

(def ERROR_CREATE_ITEM "Failed to create item")

(defn split-item-data [body-params]
  (let [field-keys (keys body-params)
        properties-keys (filter #(string/starts-with? (name %) PROPERTIES_PREFIX)
                                field-keys)
        item-keys (remove #(string/starts-with? (name %) PROPERTIES_PREFIX)
                          field-keys)
        properties (->> properties-keys
                        (map (fn [k]
                               [(-> k
                                    name
                                    (string/replace
                                     (re-pattern (str "^" PROPERTIES_PREFIX)) "")
                                    keyword)
                                (get body-params k)]))
                        (into {}))
        item-data (select-keys body-params item-keys)]
    {:item-data item-data
     :properties properties}))

(defn validate-field-permissions [request]
  (let [tx (:tx request)
        role (:role (:authenticated-entity request))
        body-params (body-params request)
        {pool-id :pool_id} (path-params request)
        item-id (:id body-params)
        permitted-fields (-> (fields/base-query "item" (keyword role) pool-id)
                             sql-format
                             (->> (jdbc-query tx)))
        permitted-field-ids (->> permitted-fields
                                 (map (comp keyword :id))
                                 set)
        body-keys (-> body-params (dissoc :id) keys set)
        unpermitted-fields (set/difference body-keys permitted-field-ids)
        {:keys [item-data]} (split-item-data body-params)
        owner-id (:owner_id item-data)]
    (cond
      (seq unpermitted-fields)
      {:error "Unpermitted fields" :unpermitted-fields unpermitted-fields}

      (or (and item-id (not= (authorized-role-for-pool request owner-id)
                             "inventory_manager")
               (not= owner-id pool-id))
          (and (not item-id) (not= owner-id pool-id)))
      {:error "Unpermitted owner_id"
       :provided owner-id
       :expected pool-id})))

(defn flatten-properties [item]
  (let [properties (:properties item)
        properties-with-prefix
        (reduce (fn [acc [k v]]
                  (assoc acc (keyword (str PROPERTIES_PREFIX (name k))) v))
                {}
                properties)
        item-without-properties (dissoc item :properties)]
    (merge item-without-properties properties-with-prefix)))

(def in-coercions
  {:retired (fn [v _] (when (true? v) (java.util.Date.)))
   :inventory_pool_id (fn [v i] (or v (:owner_id i)))})

(def out-coercions
  {:retired (fn [v _] (some? v))
   :last_check (fn [v _] (instant-to-date-string v))})

(defn coerce-field-values [item-data c-set]
  (reduce (fn [m [k c-fn]] (update m k c-fn item-data))
          item-data
          c-set))

(defn post-resource [request]
  (try
    (let [tx (:tx request)
          body-params (body-params request)
          validation-error (validate-field-permissions request)]
      (if validation-error
        (bad-request validation-error)
        (let [{:keys [item-data properties]} (split-item-data body-params)
              item-data-coerced (coerce-field-values item-data in-coercions)
              properties-json (or (not-empty properties) {})
              item-data-with-properties (assoc item-data-coerced
                                               :properties [:lift properties-json])
              sql-query (-> (sql/insert-into :items)
                            (sql/values [item-data-with-properties])
                            (sql/returning :*)
                            sql-format)
              result (jdbc/execute-one! tx sql-query)]
          (if result
            (response (-> result
                          flatten-properties
                          (coerce-field-values out-coercions)))
            (bad-request {:error ERROR_CREATE_ITEM})))))
    (catch Exception e
      (log-by-severity ERROR_CREATE_ITEM e)
      (exception-handler request ERROR_CREATE_ITEM e))))

(def ERROR_UPDATE_ITEM "Failed to update item")

(defn patch-resource [{:keys [tx] :as request}]
  (try
    (if-let [validation-error (validate-field-permissions request)]
      (bad-request validation-error)
      (let [update-params (body-params request)
            {:keys [item-data properties]} (-> update-params (dissoc :id)
                                               split-item-data)
            item-data-coerced (coerce-field-values item-data in-coercions)
            properties-json (or (not-empty properties) {})
            item-data-with-properties (assoc item-data-coerced
                                             :properties [:lift properties-json])
            sql-query (-> (sql/update :items)
                          (sql/set item-data-with-properties)
                          (sql/where [:= :id (:id update-params)])
                          (sql/returning :*)
                          sql-format)
            result (jdbc/execute-one! tx sql-query)]
        (if result
          (response (-> result
                        flatten-properties
                        (coerce-field-values out-coercions)))
          (bad-request {:error ERROR_UPDATE_ITEM}))))
    (catch Exception e
      (log-by-severity ERROR_UPDATE_ITEM e)
      (exception-handler request ERROR_UPDATE_ITEM e))))
