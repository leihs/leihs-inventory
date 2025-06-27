(ns leihs.inventory.server.resources.entitlement-groups.types
  (:require
   ;[clojure.set]
   ;[leihs.inventory.server.resources.entitlement-groups.main :refer [
   ;                                                          get-entitlement-groups-of-pool-handler
   ;                                                          ]]
   ;
   ;[leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   ;[leihs.inventory.server.utils.response_helper :as rh]
   ;[reitit.coercion.schema]
   ;[reitit.coercion.spec]
   ;[ring.middleware.accept]
   [schema.core :as s]))


(def response-body {:id s/Uuid
                    :name s/Str
                    :inventory_pool_id s/Uuid
                    :is_verification_required s/Bool
                    :created_at s/Any
                    :updated_at s/Any})
