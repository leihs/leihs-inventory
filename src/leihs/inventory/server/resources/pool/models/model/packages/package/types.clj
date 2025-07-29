(ns leihs.inventory.server.resources.pool.models.model.packages.package.types
  (:require
   [clojure.spec.alpha :as sa]
   [leihs.inventory.server.resources.pool.models.basic_coercion :as sp]
   [reitit.coercion.schema]
   [schema.core :as s]
   [spec-tools.core :as st]))

(sa/def ::data
  (st/spec {:spec (sa/keys :req-un [::sp/inventory_code
                                    :nil/retired
                                    ::sp/is_borrowable
                                    ::sp/is_inventory_relevant
                                    ::sp/is_broken
                                    ::sp/is_incomplete
                                    :nil/last_check
                                    :nil/shelf
                                    :nil/status_note
                                    :nil/user_name
                                    ::sp/room_id
                                    ::sp/model_id
                                    ::sp/owner_id
                                    :nil-number/price]

                           :opt-un [::sp/note
                                    ::sp/items_attributes
                                    :nil/name
                                    :nil/invoice_number
                                    ::sp/properties
                                    ::sp/updated_at
                                    :nil/retired_reason
                                    :nil/note
                                    :nil/responsible
                                    :nil/invoice_date
                                    :nil/supplier_id
                                    :nil/parent_id
                                    ::sp/id
                                    ::sp/inventory_pool_id
                                    :nil/item_version
                                    ::sp/needs_permission
                                    :nil/serial_number
                                    ::sp/created_at
                                    :nil/insurance_number])

            :description "Inventory item data"}))

(sa/def ::fields (sa/coll-of map? :kind vector?))

(sa/def :package-put-response3/inventory-item
  (st/spec {:spec (sa/keys :req-un [::data]
                           :opt-un [::fields])
            :description "Complete inventory response"}))

(sa/def :package-put-response2/inventory-item
  (st/spec ::data))

(sa/def :package-get-response/inventory-item
  (st/spec {:data {:inventory_code string?
                   :inventory_pool_id uuid?
                   :responsible_department any?}
            :fields [any?]}))
