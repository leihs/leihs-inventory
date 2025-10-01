(ns leihs.inventory.server.resources.pool.items.types
  (:require
   [leihs.inventory.server.resources.types :refer [pagination]]
   [schema.core :as s]))

(s/defschema path-params {:pool_id s/Uuid})

(s/defschema query-params {(s/optional-key :fields) s/Str

                           (s/optional-key :search_term) s/Str
                           (s/optional-key :model_id) s/Uuid
                           (s/optional-key :parent_id) s/Uuid

                           ;; item filters
                           (s/optional-key :borrowable) s/Bool
                           (s/optional-key :broken) s/Bool
                           (s/optional-key :in_stock) s/Bool
                           (s/optional-key :incomplete) s/Bool
                           (s/optional-key :retired) s/Bool
                           (s/optional-key :owned) s/Bool
                           (s/optional-key :inventory_pool_id) s/Uuid

                           (s/optional-key :page) s/Int
                           (s/optional-key :size) s/Int})

(s/defschema item
  {(s/optional-key :id) s/Uuid
   (s/optional-key :inventory_code) s/Str
   (s/optional-key :serial_number) (s/maybe s/Str)
   (s/optional-key :model_id) s/Uuid
   (s/optional-key :supplier_id) (s/maybe s/Uuid)
   (s/optional-key :owner_id) s/Uuid
   (s/optional-key :inventory_pool_id) s/Uuid
   (s/optional-key :parent_id) (s/maybe s/Uuid)
   (s/optional-key :invoice_number) (s/maybe s/Str)
   (s/optional-key :invoice_date) (s/maybe java.util.Date)
   (s/optional-key :last_check) (s/maybe java.util.Date)
   (s/optional-key :retired) (s/maybe java.util.Date)
   (s/optional-key :retired_reason) (s/maybe s/Str)
   (s/optional-key :price) (s/maybe s/Num) ; numeric(8,2)
   (s/optional-key :is_broken) (s/maybe s/Bool)
   (s/optional-key :is_incomplete) (s/maybe s/Bool)
   (s/optional-key :is_borrowable) (s/maybe s/Bool)
   (s/optional-key :status_note) (s/maybe s/Str)
   (s/optional-key :needs_permission) (s/maybe s/Bool)
   (s/optional-key :is_inventory_relevant) (s/maybe s/Bool)
   (s/optional-key :responsible) (s/maybe s/Str)
   (s/optional-key :insurance_number) (s/maybe s/Str)
   (s/optional-key :note) (s/maybe s/Str)
   (s/optional-key :name) (s/maybe s/Str)
   (s/optional-key :user_name) (s/maybe s/Str)
   (s/optional-key :shelf) (s/maybe s/Str)
   (s/optional-key :room_id) s/Uuid
   (s/optional-key :properties) {s/Keyword s/Any} ; jsonb
   (s/optional-key :item_version) (s/maybe s/Str)
   (s/optional-key :is_package) s/Bool
   (s/optional-key :room_description) (s/maybe s/Str)
   (s/optional-key :reservation_end_date) (s/maybe s/Str)
   (s/optional-key :building_code) (s/maybe s/Str)
   (s/optional-key :inventory_pool_name) (s/maybe s/Str)
   (s/optional-key :package_items) (s/maybe s/Int)
   (s/optional-key :model_name) (s/maybe s/Str)
   (s/optional-key :building_name) (s/maybe s/Str)
   (s/optional-key :reservation_user_id) (s/maybe s/Uuid)
   (s/optional-key :reservation_user_name) (s/maybe s/Str)
   (s/optional-key :room_name) (s/maybe s/Str)

   (s/optional-key :created_at) java.util.Date
   (s/optional-key :updated_at) java.util.Date})

(def get-items-response
  (s/->Either [[item] {:data [item] :pagination pagination}]))
