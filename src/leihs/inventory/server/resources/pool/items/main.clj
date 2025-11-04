(ns leihs.inventory.server.resources.pool.items.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.items.shared :as items-shared]
   [leihs.inventory.server.resources.pool.items.types :as types]
   [leihs.inventory.server.resources.pool.models.common :refer [fetch-thumbnails-for-ids
                                                                filter-map-by-spec
                                                                model->enrich-with-image-attr]]
   [leihs.inventory.server.utils.debug :refer [log-by-severity]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [leihs.inventory.server.utils.pick-fields :refer [pick-fields]]

   [leihs.inventory.server.utils.request-utils :refer [path-params
                                                       query-params]]
   [ring.middleware.accept]
   [ring.util.response :refer [response]]
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
           (pick-fields fields types/item)
           response)
       (catch Exception e
         (log-by-severity ERROR_GET_ITEMS e)
         (exception-handler request ERROR_GET_ITEMS e))))))
