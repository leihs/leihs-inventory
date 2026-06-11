(ns leihs.inventory.server.resources.pool.list.export
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.items.shared :as items-shared]
   [next.jdbc :as jdbc]))

(defn- categories-aggregation []
  (-> (sql/select :model_links.model_id
                  [[:string_agg
                    [:distinct
                     [:coalesce :model_group_links.label :model_groups.name]]
                    "; "]
                   :categories])
      (sql/from :model_groups)
      (sql/join :model_group_links
                [:= :model_groups.id :model_group_links.child_id])
      (sql/join :model_links
                [:= :model_groups.id :model_links.model_group_id])
      (sql/where [:= :model_groups.type "Category"])
      (sql/group-by :model_links.model_id)))

(defn- accessories-aggregation []
  (-> (sql/select :accessories.model_id
                  [[:string_agg :accessories.name "; "] :accessories])
      (sql/from :accessories)
      (sql/group-by :accessories.model_id)))

(defn- compatibles-aggregation []
  (-> (sql/select :models_compatibles.model_id
                  [[:string_agg :compatibles.name "; "] :compatibles])
      (sql/from :models_compatibles)
      (sql/join [:models :compatibles]
                [:= :models_compatibles.compatible_id :compatibles.id])
      (sql/group-by :models_compatibles.model_id)))

(defn- model-properties-aggregation []
  (-> (sql/select :properties.model_id
                  [[:string_agg
                    [:concat_ws ": " :properties.key :properties.value]
                    "; "]
                   :properties])
      (sql/from :properties)
      (sql/group-by :properties.model_id)))

(def select-model-fields
  [:inventory.product
   :inventory.version
   :inventory.manufacturer
   :models.description
   :models.technical_detail
   :models.internal_description
   :models.hand_over_note
   :export_categories.categories
   :export_accessories.accessories
   :export_compatibles.compatibles
   :export_model_properties.properties])

(def select-item-fields
  [[[:coalesce :items.inventory_code :inventory.inventory_code] :inventory_code]
   :items.serial_number
   [:suppliers.name :supplier]
   [:export_owner_pool.name :owner]
   [:export_responsible_pool.name :responsible]
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
   [:export_buildings.name :building]
   [:export_rooms.name :room]
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

(defn export-aggregation-joins [query]
  (-> query
      (sql/left-join [(categories-aggregation) :export_categories]
                     [:= :export_categories.model_id :models.id])
      (sql/left-join [(accessories-aggregation) :export_accessories]
                     [:= :export_accessories.model_id :models.id])
      (sql/left-join [(compatibles-aggregation) :export_compatibles]
                     [:= :export_compatibles.model_id :models.id])
      (sql/left-join [(model-properties-aggregation) :export_model_properties]
                     [:= :export_model_properties.model_id :models.id])))

(defn export-item-joins [query]
  (-> query
      (sql/left-join :suppliers [:= :suppliers.id :items.supplier_id])
      (sql/left-join [:inventory_pools :export_owner_pool]
                     [:= :export_owner_pool.id :items.owner_id])
      (sql/left-join [:inventory_pools :export_responsible_pool]
                     [:= :export_responsible_pool.id
                      [:coalesce :items.inventory_pool_id :inventory.inventory_pool_id]])
      (sql/left-join [:rooms :export_rooms] [:= :export_rooms.id :items.room_id])
      (sql/left-join [:buildings :export_buildings]
                     [:= :export_buildings.id :export_rooms.building_id])))

(comment
  (require '[leihs.core.db :as db])
  (get-active-property-fields (db/get-ds)
                              #uuid "8bd16d45-056d-5590-bc7f-12849f034351")
  (sql-format (property-field-select "ampere" "properties_ampere")))

(defn sql-prepare
  [tx query pool-id {:keys [with_items item-opts]}]
  (let [property-fields (get-active-property-fields tx pool-id)
        property-selects (map (fn [{:keys [id key]}]
                                (property-field-select key id))
                              property-fields)
        items-join-cond (items-shared/items-join-conditions pool-id
                                                            (true? with_items)
                                                            item-opts)
        join-items (if (and (true? with_items) item-opts)
                     #(sql/join % :items items-join-cond)
                     #(sql/left-join % :items items-join-cond))]
    (-> query
        (dissoc :select)
        (#(apply sql/select %
                 type-expr
                 (concat select-model-fields
                         select-item-fields
                         property-selects
                         timestamps)))
        (sql/left-join :models [:and [:= :inventory.id :models.id]])
        (export-aggregation-joins)
        (sql/left-join :options [:and [:= :inventory.id :options.id]])
        (join-items)
        (export-item-joins)
        (sql/left-join :reservations [:and
                                      [:= :items.id :reservations.item_id]
                                      [:= :reservations.status "signed"]
                                      [:is :reservations.returned_date nil]])
        (sql/left-join :users [:= :reservations.user_id :users.id])
        (sql/left-join [:users :delegated_users]
                       [:= :reservations.delegated_user_id :delegated_users.id])
        (sql/order-by :items.inventory_code))))
