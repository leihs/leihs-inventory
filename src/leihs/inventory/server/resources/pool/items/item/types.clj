(ns leihs.inventory.server.resources.pool.items.item.types
  (:require
   [leihs.inventory.server.resources.pool.items.types :refer [post-response properties]]
   [leihs.inventory.server.utils.schema :refer [Date]]
   [schema.core :as s]))

(def get-response post-response)

(def patch-request
  (merge {(s/optional-key :inventory_code) s/Str
          (s/optional-key :model_id) s/Uuid
          (s/optional-key :owner_id) s/Uuid
          (s/optional-key :room_id) s/Uuid
          (s/optional-key :insurance_number) (s/maybe s/Str)
          (s/optional-key :inventory_pool_id) s/Uuid
          (s/optional-key :invoice_date) (s/maybe Date)
          (s/optional-key :invoice_number) (s/maybe s/Str)
          (s/optional-key :is_borrowable) s/Bool
          (s/optional-key :is_broken) s/Bool
          (s/optional-key :is_incomplete) s/Bool
          (s/optional-key :is_inventory_relevant) s/Bool
          (s/optional-key :item_ids) [s/Uuid]
          (s/optional-key :item_version) (s/maybe s/Str)
          (s/optional-key :last_check) (s/maybe Date)
          (s/optional-key :name) (s/maybe s/Str)
          (s/optional-key :needs_permission) s/Bool
          (s/optional-key :note) (s/maybe s/Str)
          (s/optional-key :parent_id) (s/maybe s/Uuid)
          (s/optional-key :price) (s/maybe s/Str)
          (s/optional-key :responsible) (s/maybe s/Str)
          (s/optional-key :retired_reason) (s/maybe s/Str)
          (s/optional-key :retired) s/Bool
          (s/optional-key :serial_number) (s/maybe s/Str)
          (s/optional-key :shelf) (s/maybe s/Str)
          (s/optional-key :status_note) (s/maybe s/Str)
          (s/optional-key :supplier_id) (s/maybe s/Uuid)
          (s/optional-key :user_name) (s/maybe s/Str)} properties))

(def patch-response post-response)
