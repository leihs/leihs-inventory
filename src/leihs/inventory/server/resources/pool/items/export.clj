(ns leihs.inventory.server.resources.pool.items.export
  (:require
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.list.export :as list-export]))

(def select-model-fields
  [:models.product
   :models.version
   :models.manufacturer
   :models.description
   :models.technical_detail
   :models.internal_description
   :models.hand_over_note
   :export_categories.categories
   :export_accessories.accessories
   :export_compatibles.compatibles
   :export_model_properties.properties])

(def select-item-fields
  [:items.inventory_code
   :items.serial_number
   [:suppliers.name :supplier]
   [:export_owner_pool.name :owner]
   [:export_responsible_pool.name :responsible]
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
   [:export_buildings.name :building]
   [:export_rooms.name :room]
   :items.shelf])

(def timestamps
  [:items.created_at
   :items.updated_at])

(defn- export-item-joins [query]
  (-> query
      (sql/left-join :suppliers [:= :suppliers.id :items.supplier_id])
      (sql/left-join [:inventory_pools :export_owner_pool]
                     [:= :export_owner_pool.id :items.owner_id])
      (sql/left-join [:inventory_pools :export_responsible_pool]
                     [:= :export_responsible_pool.id :items.inventory_pool_id])
      (sql/left-join [:rooms :export_rooms] [:= :export_rooms.id :items.room_id])
      (sql/left-join [:buildings :export_buildings]
                     [:= :export_buildings.id :export_rooms.building_id])))

(defn sql-prepare [tx query pool-id]
  (let [property-fields (list-export/get-active-property-fields tx pool-id)
        property-selects (map (fn [{:keys [id key]}]
                                (list-export/property-field-select key id))
                              property-fields)]
    (-> query
        (dissoc :select)
        (#(apply sql/select %
                 (concat select-model-fields
                         select-item-fields
                         property-selects
                         timestamps)))
        (list-export/export-aggregation-joins)
        (export-item-joins)
        (sql/order-by :items.inventory_code))))
