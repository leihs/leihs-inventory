(ns leihs.inventory.server.resources.pool.items.main
  (:require
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.inventory-code :as inv-code]
   [leihs.inventory.server.resources.pool.items.fields-shared :refer [coerce-field-values
                                                                      flatten-properties
                                                                      in-coercions
                                                                      out-coercions
                                                                      split-item-data
                                                                      validate-field-permissions]]
   [leihs.inventory.server.resources.pool.items.shared :as items-shared]
   [leihs.inventory.server.resources.pool.items.types :as types]
   [leihs.inventory.server.resources.pool.list.search :refer [with-search]]
   [leihs.inventory.server.resources.pool.models.common :refer [fetch-thumbnails-for-ids]]
   [leihs.inventory.server.utils.debug :refer [log-by-severity]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [leihs.inventory.server.utils.pick-fields :refer [pick-fields]]
   [leihs.inventory.server.utils.request-utils :refer [body-params path-params
                                                       query-params]]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [debug]]))

(def ERROR_GET_ITEMS "Failed to get items")

(def base-query
  (-> (->> types/columns
           (map #(keyword "items" (name %)))
           concat
           (apply sql/select))
      (sql/from :items)
      (sql/order-by :items.inventory_code)))

(defn index-resources
  ([request]
   (let [tx (:tx request)
         {:keys [pool_id]} (path-params request)
         {:keys [fields search
                 model_id parent_id only_items
                 retired borrowable
                 incomplete broken owned
                 inventory_pool_id
                 in_stock before_last_check]} (query-params request)

         extra-columns [[:ip.name :inventory_pool_name]
                        [:r.end_date :reservation_end_date]
                        [:r.user_id :reservation_user_id]
                        [:r.contract_id :reservation_contract_id]
                        [:models.is_package :is_package]
                        [:models.name :model_name]
                        [:rs.name :room_name]
                        [:rs.description :room_description]
                        [:b.name :building_name]
                        [:b.code :building_code]

                        [[:nullif [:concat_ws " " :u.firstname :u.lastname] ""] :reservation_user_name]

                        [(-> (sql/select :%count.*) ; [[:count :*]]
                             (sql/from [:items :i])
                             (sql/where [:= :i.parent_id :items.id]))
                         :package_items]]

         query (-> base-query
                   (#(apply sql/select % extra-columns))

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
                   (sql/left-join :models
                                  [:or
                                   [:= :items.model_id :models.id]
                                   [:= :models.id model_id]])

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
                   (cond-> only_items (sql/where [:and
                                                  [:not= :models.is_package true]
                                                  [:= :items.parent_id nil]
                                                  [:not= :models.type "Software"]]))

                   ; in legacy no query params are passed down to the children,
                   ; speaking: all children are always showed.
                   (cond-> (not parent_id)
                     (-> (items-shared/item-query-params pool_id
                                                         :inventory_pool_id inventory_pool_id
                                                         :owned owned
                                                         :in_stock in_stock
                                                         :before_last_check before_last_check
                                                         :retired retired
                                                         :borrowable borrowable
                                                         :broken broken
                                                         :incomplete incomplete)
                         (cond-> (seq search) (with-search search :models)))))

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

(defn inventory-code-exists? [tx inventory-code exclude-id]
  (let [query (-> (sql/select :id)
                  (sql/from :items)
                  (sql/where [:= :inventory_code inventory-code])
                  (cond-> exclude-id
                    (sql/where [:not= :id exclude-id]))
                  sql-format)]
    (-> (jdbc/execute-one! tx query)
        some?)))

(defn validate-item-ids-for-package [tx item-ids]
  (-> (sql/select :items.id)
      (sql/from :items)
      (sql/join :models [:= :models.id :items.model_id])
      (sql/where [:in :items.id item-ids])
      (sql/where [:or
                  [:= :models.is_package true]
                  [:not= :items.parent_id nil]])
      sql-format
      (->> (jdbc-query tx))))

(defn post-resource [request]
  (try
    (let [tx (:tx request)
          {:keys [pool_id]} (path-params request)
          body-params (body-params request)
          validation-error (validate-field-permissions request)]
      (if validation-error
        (bad-request validation-error)
        (let [{:keys [item-data properties]} (split-item-data body-params)
              item-type (:type body-params)
              item-ids (:item_ids body-params)
              inventory-code (:inventory_code item-data)
              model-id (:model_id item-data)]
          (when (= item-type "package")
            (let [model (-> (sql/select :is_package)
                            (sql/from :models)
                            (sql/where [:= :id model-id])
                            sql-format
                            (->> (jdbc/execute-one! tx)))]
              (when-not (:is_package model)
                (throw (ex-info "Model must have is_package=true for package type"
                                {:model_id model-id})))))
          (if (inventory-code-exists? tx inventory-code nil)
            (status {:body {:error "Inventory code already exists"
                            :proposed_code (inv-code/propose tx pool_id)}}
                    409)
            (let [item-data-coerced (coerce-field-values item-data in-coercions)
                  properties-json (or (not-empty properties) {})
                  item-data-with-properties (assoc item-data-coerced
                                                   :properties [:lift properties-json])
                  sql-query (-> (sql/insert-into :items)
                                (sql/values [item-data-with-properties])
                                (sql/returning :*)
                                sql-format)
                  result (jdbc/execute-one! tx sql-query)]
              (if result
                (do
                  (when (= item-type "package")
                    (when-not (seq item-ids)
                      (throw (ex-info "item_ids is required for package" {})))
                    (let [invalid-items (validate-item-ids-for-package tx item-ids)]
                      (when (seq invalid-items)
                        (throw (ex-info "Cannot add packages or already assigned items to package"
                                        {:invalid_item_ids (map :id invalid-items)}))))
                    (-> (sql/update :items)
                        (sql/set {:parent_id (:id result)})
                        (sql/where [:in :id item-ids])
                        sql-format
                        (->> (jdbc/execute! tx))))
                  (response (-> result
                                flatten-properties
                                (coerce-field-values out-coercions)
                                (cond-> (= item-type "package")
                                  (assoc :item_ids item-ids)))))
                (bad-request {:error ERROR_CREATE_ITEM})))))))
    (catch Exception e
      (log-by-severity ERROR_CREATE_ITEM e)
      (exception-handler request ERROR_CREATE_ITEM e))))
