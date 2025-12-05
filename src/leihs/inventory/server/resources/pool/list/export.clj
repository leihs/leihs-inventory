(ns leihs.inventory.server.resources.pool.list.export
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.items.shared :as items-shared]
   [next.jdbc :as jdbc]))

(def select-model-fields
  [:inventory.product
   :inventory.version
   :inventory.manufacturer
   :models.description
   :models.technical_detail
   :models.internal_description
   :models.hand_over_note

   ; categories
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

   ; accessories
   [(-> (sql/select [[:string_agg :accessories.name "; "]])
        (sql/from :accessories)
        (sql/where [:= :accessories.model_id :models.id])
        (sql/group-by :accessories.model_id)) :accessories]

   ; models-compatibles
   [(-> (sql/select [[:string_agg :compatibles.name "; "]])
        (sql/from :models_compatibles)
        (sql/join [:models :compatibles]
                  [:= :models_compatibles.compatible_id :compatibles.id])
        (sql/where [:= :models_compatibles.model_id :models.id])
        (sql/group-by :models_compatibles.model_id)) :compatibles]

   ; properties
   [(-> (sql/select [[:string_agg
                      [:concat_ws ": " :properties.key
                       :properties.value]
                      "; "]])
        (sql/from :properties)
        (sql/where [:= :properties.model_id :models.id])
        (sql/group-by :properties.model_id)) :properties]])

(def select-item-fields
  [[[:coalesce :items.inventory_code :inventory.inventory_code] :inventory_code]
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
        (sql/where [:or
                    [:= :inventory_pools.id :items.inventory_pool_id]
                    [:= :inventory_pools.id :inventory.inventory_pool_id]]))
    :responsible]
   :items.invoice_number
   :items.invoice_date
   :items.last_check
   :items.retired
   :items.retired_reason
   [[:coalesce :items.price :inventory.price] :price]
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
   :items.shelf
   [[:coalesce :delegated_users.firstname :users.firstname] :firstname]
   [[:coalesce :delegated_users.lastname :users.lastname] :lastname]
   [[:coalesce :delegated_users.badge_id :users.badge_id] :badge_id]
   [[:case
     [:is-not :delegated_users.id nil] :users.firstname
     :else nil] :delegation_name]
   [:reservations.end_date :lended_until]])

(def timestamps
  [[[:coalesce :items.created_at :inventory.created_at]
    :created_at]
   [[:coalesce :items.updated_at :inventory.updated_at]
    :updated_at]])

(defn get-active-property-fields
  "Fetch all active property fields for a pool (excluding disabled ones)"
  [tx pool-id]
  (-> (sql/select :fields.id
                  [[:cast :fields.data :jsonb] :data])
      (sql/from :fields)
      (sql/where [:= :fields.active true]
                 [:like :fields.id "properties_%"]
                 [:not [:exists
                        (-> (sql/select 1)
                            (sql/from :disabled_fields)
                            (sql/where [:= :disabled_fields.field_id :fields.id]
                                       [:= :disabled_fields.inventory_pool_id pool-id]))]])
      (sql/order-by :fields.position)
      sql-format
      (->> (jdbc/execute! tx)
           (map (fn [field]
                  {:id (:id field)
                   :key (get-in field [:data "attribute" 1])})))))

(defn property-field-select
  "Generate a SELECT clause for a property field"
  [property-key field-id]
  [[:-> :items.properties property-key] (keyword field-id)])

(def type-expr
  [[:case
    [:and [:is-not :items.inventory_code nil] [:= :inventory.type "Package"]]
    "Package-Item"
    [:and [:is-not :items.inventory_code nil] [:= :inventory.type "Model"]]
    "Item"
    [:= :inventory.type "Package"]
    "Package-Model"
    :else :inventory.type] :type])

(comment
  (require '[leihs.core.db :as db])
  (get-active-property-fields (db/get-ds)
                              #uuid "8bd16d45-056d-5590-bc7f-12849f034351")
  (sql-format (property-field-select "ampere" "properties_ampere")))

(defn sql-prepare [tx query pool-id]
  (let [property-fields (get-active-property-fields tx pool-id)
        property-selects (map (fn [{:keys [id key]}]
                                (property-field-select key id))
                              property-fields)]
    (-> query
        (dissoc :select)
        (#(apply sql/select %
                 type-expr
                 (concat select-model-fields
                         select-item-fields
                         property-selects
                         timestamps)))
        (sql/left-join :models [:and [:= :inventory.id :models.id]])
        (sql/left-join :options [:and [:= :inventory.id :options.id]])
        (sql/left-join :items [:and [:= :inventory.id :items.model_id]
                               (items-shared/owner-or-responsible-cond pool-id)])
        (sql/left-join :reservations [:and
                                      [:= :items.id :reservations.item_id]
                                      [:= :reservations.status "signed"]
                                      [:is :reservations.returned_date nil]])
        (sql/left-join :users [:= :reservations.user_id :users.id])
        (sql/left-join [:users :delegated_users]
                       [:= :reservations.delegated_user_id :delegated_users.id])
        (sql/order-by :items.inventory_code))))
