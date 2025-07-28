(ns leihs.inventory.server.resources.pool.models.model.items.types
  (:require
   [leihs.inventory.server.resources.types :refer [pagination]]
   [reitit.coercion.schema]
   [ring.middleware.accept]
   [schema.core :as s]))

(def DateTime (s/cond-pre s/Str java.util.Date java.sql.Timestamp))

(def item
  {:id (s/maybe s/Uuid)
   :model_id (s/maybe s/Uuid)
   :parent_id (s/maybe s/Uuid)
   :inventory_pool_id (s/maybe s/Uuid)
   :owner_id (s/maybe s/Uuid)
   :supplier_id (s/maybe s/Uuid)
   :room_id (s/maybe s/Uuid)
   :properties (s/maybe s/Any)
   :inventory_code (s/maybe s/Str)
   :is_borrowable (s/maybe s/Bool)
   :is_inventory_relevant (s/maybe s/Bool)
   :is_broken (s/maybe s/Bool)
   :is_incomplete (s/maybe s/Bool)
   :needs_permission (s/maybe s/Bool)
   :serial_number (s/maybe s/Str)
   :shelf (s/maybe s/Str)
   :retired (s/maybe DateTime)
   :retired_reason (s/maybe s/Str)
   :status_note (s/maybe s/Str)
   :name (s/maybe s/Str)
   :invoice_number (s/maybe s/Str)
   :invoice_date (s/maybe DateTime)
   :user_name (s/maybe s/Str)
   :note (s/maybe s/Str)
   :updated_at (s/maybe DateTime)
   :created_at (s/maybe DateTime)
   :item_version (s/maybe s/Str)
   :price (s/maybe s/Num)
   :insurance_number (s/maybe s/Str)
   :last_check (s/maybe DateTime)
   :responsible (s/maybe s/Str)
   (s/optional-key :is_package) (s/maybe s/Bool)
   (s/optional-key :building_name) (s/maybe s/Str)
   (s/optional-key :room_name) (s/maybe s/Str)})

(def get-items-response {:data [item]
                         :pagination pagination})
