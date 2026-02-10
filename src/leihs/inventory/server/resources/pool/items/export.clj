(ns leihs.inventory.server.resources.pool.items.export
  (:require
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.list.export :refer [get-active-property-fields
                                                              property-field-select]]))

(def select-model-fields
  [:models.product
   :models.version
   :models.manufacturer
   :models.description
   :models.technical_detail
   :models.internal_description
   :models.hand_over_note

   ;; categories
   [(-> (sql/select [[:string_agg
                      [:distinct
                       [:coalesce :model_group_links.label
                        :model_groups.name]]
                      "; "]])
        (sql/from :model_groups)
        (sql/join :model_group_links
                  [:= :model_groups.id :model_group_links.child_id])
        (sql/join :model_links
                  [:= :model_groups.id :model_links.model_group_id])
        (sql/where [:= :model_groups.type "Category"])
        (sql/where [:= :model_links.model_id :models.id])
        (sql/group-by :model_links.model_id)) :categories]

   ;; accessories
   [(-> (sql/select [[:string_agg :accessories.name "; "]])
        (sql/from :accessories)
        (sql/where [:= :accessories.model_id :models.id])
        (sql/group-by :accessories.model_id)) :accessories]

   ;; compatibles
   [(-> (sql/select [[:string_agg :compatibles.name "; "]])
        (sql/from :models_compatibles)
        (sql/join [:models :compatibles]
                  [:= :models_compatibles.compatible_id :compatibles.id])
        (sql/where [:= :models_compatibles.model_id :models.id])
        (sql/group-by :models_compatibles.model_id)) :compatibles]

   ;; model properties
   [(-> (sql/select [[:string_agg
                      [:concat_ws ": " :properties.key
                       :properties.value]
                      "; "]])
        (sql/from :properties)
        (sql/where [:= :properties.model_id :models.id])
        (sql/group-by :properties.model_id)) :properties]])

(def select-item-fields
  [:items.inventory_code
   :items.serial_number
   [(-> (sql/select :suppliers.name)
        (sql/from :suppliers)
        (sql/where [:= :suppliers.id :items.supplier_id]))
    :supplier]
   [(-> (sql/select :inventory_pools.name)
        (sql/from :inventory_pools)
        (sql/where [:= :inventory_pools.id :items.owner_id]))
    :owner]
   [(-> (sql/select :inventory_pools.name)
        (sql/from :inventory_pools)
        (sql/where [:= :inventory_pools.id :items.inventory_pool_id]))
    :responsible]
   :items.invoice_number
   :items.invoice_date
   :items.last_check
   :items.retired
   :items.retired_reason
   :items.price
   :items.is_broken
   :items.is_incomplete
   :items.is_borrowable
   :items.status_note
   :items.needs_permission
   :items.is_inventory_relevant
   :items.insurance_number
   :items.note
   :items.name
   :items.user_name
   :items.item_version
   [(-> (sql/select :buildings.name)
        (sql/from :buildings)
        (sql/join :rooms [:= :rooms.building_id :buildings.id])
        (sql/where [:= :rooms.id :items.room_id])) :building]
   [(-> (sql/select :rooms.name)
        (sql/from :rooms)
        (sql/where [:= :rooms.id :items.room_id])) :room]
   :items.shelf])

(def timestamps
  [:items.created_at
   :items.updated_at])

(defn sql-prepare [tx query pool-id]
  (let [property-fields (get-active-property-fields tx pool-id)
        property-selects (map (fn [{:keys [id key]}]
                                (property-field-select key id))
                              property-fields)]
    (-> query
        (dissoc :select)
        (#(apply sql/select %
                 (concat select-model-fields
                         select-item-fields
                         property-selects
                         timestamps)))
        (sql/order-by :items.inventory_code))))
