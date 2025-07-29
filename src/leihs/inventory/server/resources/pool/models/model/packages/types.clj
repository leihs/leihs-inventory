(ns leihs.inventory.server.resources.pool.models.model.packages.types
  (:require
   [clojure.spec.alpha :as sa]
   [leihs.inventory.server.resources.pool.models.basic_coercion :as sp]
   [reitit.coercion.schema]
   [schema.core :as s]
   [spec-tools.core :as st]))

(sa/def :package/payload (sa/keys :req-un [::sp/model_id
                                           ::sp/room_id]
                                  :opt-un [::sp/owner_id
                                           :nil-number/price
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
