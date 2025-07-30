(ns leihs.inventory.server.resources.pool.models.model.packages.package.types
  (:require
   [clojure.spec.alpha :as sa]
   [leihs.inventory.server.resources.pool.models.basic_coercion :as sp]
   ;[leihs.inventory.server.resources.types :refer [pagination]]
   [reitit.coercion.schema]
   [schema.core :as s]
   [spec-tools.core :as st]))

(sa/def :package/payload (sa/keys :req-un [::sp/model_id
                                           ::sp/room_id]
                                  :opt-un [::sp/owner_id
                                           :nil/price
                                           :nil/shelf
                                           :nil/status_note
                                           :nil/note
                                           ::sp/last_check
                                           ::sp/inventory_code
                                           ::sp/retired
                                           ::sp/is_broken
                                           ::sp/is_incomplete
                                           ::sp/is_borrowable
                                           :any/items_attributes
                                           :nil/retired_reason
                                           ::sp/is_inventory_relevant
                                           :nil/user_name]))



(sa/def ::data
  (st/spec {:spec (sa/keys :req-un [:sp/inventory_code
                                    :nil/retired
                                    :sp/is_borrowable
                                    :sp/is_inventory_relevant
                                    :sp/is_broken
                                    :sp/is_incomplete
                                    :nil/last_check
                                    :nil/shelf
                                    :nil/status_note
                                    :nil/user_name
                                    :sp/room_id
                                    :sp/model_id
                                    :sp/owner_id
                                    :sp/price]

                    :opt-un [:sp/note
                             :sp/items_attributes
                             :sp/name
                             :sp/invoice_number
                             :sp/properties
                             :sp/updated_at
                             :nil/retired_reason
                             :nil/note
                             :sp/responsible
                             :sp/invoice_date
                             :sp/supplier_id
                             :sp/parent_id
                             :sp/id
                             :sp/inventory_pool_id
                             :sp/item_version
                             :sp/needs_permission
                             :sp/serial_number
                             :sp/created_at
                             :sp/insurance_number
                             :sp/insurance_number])

            :description "Inventory item data"}))


;(sa/def :package-put-response2/inventory-item
;  (st/spec {:spec (sa/keys :req-un [:res/data]
;                    :opt-un [:res/validation])
;            :description "Complete inventory response"}))


(sa/def :package-put-response2/inventory-item
  (st/spec ::data))