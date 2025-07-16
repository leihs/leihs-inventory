(ns leihs.inventory.server.resources.pool.fields.types
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.pool.fields.main :refer [index-resources]]
   [leihs.inventory.server.resources.types :refer [pagination]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(s/defschema query-params {:id s/Uuid
                           :name s/Str
                           :inventory_pool_id s/Uuid
                           :is_verification_required s/Bool
                           :created_at s/Any
                           :updated_at s/Any})

(def Permissions
  {(s/optional-key :role) s/Str
   (s/optional-key :owner) s/Bool})

(def Data
  {(s/optional-key :type) s/Str
   (s/optional-key :group) (s/maybe s/Str)
   (s/optional-key :label) s/Str
   (s/optional-key :required) s/Bool
   (s/optional-key :attribute) (s/cond-pre s/Str [s/Str])
   (s/optional-key :forPackage) s/Bool
   (s/optional-key :permissions) Permissions
   ;s/Keyword s/Any
}) ;; allow extra keys

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

(def get-response (s/->Either [{:data [item]
                                :pagination pagination} [item]]))
