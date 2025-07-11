(ns leihs.inventory.server.resources.pool.models.model.items.types
  (:require
   [clojure.spec.alpha :as sa]
   [clojure.string :as str]
   [leihs.inventory.server.resources.pool.models.coercion :as mc]
   [leihs.inventory.server.resources.pool.models.model.items.main :refer [index-resources]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.auth.role-auth :refer [permission-by-role-and-pool]]
   [leihs.inventory.server.utils.auth.roles :as roles]
   [leihs.inventory.server.utils.coercion.core :refer [Date]]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [ring.middleware.accept]
   [schema.core :as s]))


(require '[schema.core :as s])

(def DateTime (s/cond-pre s/Str java.util.Date java.sql.Timestamp))

(def item
  {:id                       s/Uuid
   :model_id                 s/Uuid
   :parent_id                (s/maybe s/Uuid)
   :inventory_pool_id        s/Uuid
   :owner_id                 s/Uuid
   :supplier_id              s/Uuid
   :room_id                  s/Uuid
   ;:properties               {s/Str s/Str}
   :properties               s/Any
   :inventory_code           s/Str
   :is_borrowable            s/Bool
   :is_inventory_relevant    s/Bool
   :is_broken                s/Bool
   :is_incomplete            s/Bool
   :needs_permission         s/Bool
   :serial_number            s/Str
   :shelf (s/maybe s/Str)
   :retired                  (s/maybe DateTime)
   :retired_reason           (s/maybe s/Str)
   :status_note              (s/maybe s/Str)
   :name                     (s/maybe s/Str)
   :invoice_number           (s/maybe s/Str)
   :invoice_date             (s/maybe DateTime)
   :user_name                (s/maybe s/Str)
   :note                     (s/maybe s/Str)
   :updated_at               DateTime
   :created_at               DateTime
   :item_version             (s/maybe s/Str)
   :price                    s/Num
   :insurance_number         (s/maybe s/Str)
   :last_check               (s/maybe DateTime)
   :responsible              (s/maybe s/Str)
   })



(def get-items-response {
                         :data [item]
                         :pagination s/Any
                         })

(def get-item-response item)