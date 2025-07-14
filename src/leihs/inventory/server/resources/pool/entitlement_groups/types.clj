(ns leihs.inventory.server.resources.pool.entitlement-groups.types
  (:require
   [schema.core :as s]))

(def response-body {:id s/Uuid
                    :name s/Str
                    :inventory_pool_id s/Uuid
                    :is_verification_required s/Bool
                    :created_at s/Any
                    :updated_at s/Any})
