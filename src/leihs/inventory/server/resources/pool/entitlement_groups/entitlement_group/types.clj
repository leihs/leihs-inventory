(ns leihs.inventory.server.resources.pool.entitlement-groups.entitlement-group.types
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.pool.entitlement-groups.types :refer [model get-model]]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(def delete-response-body {:entitlement_groups s/Any
                           :models s/Any})

(def put-response-body {:entitlement_group {:id s/Uuid
                                            :name s/Str
                                            :is_verification_required s/Bool
                                            :inventory_pool_id s/Uuid
                                            :created_at s/Any
                                            :updated_at s/Any}
                        :models {:deleted [model]
                                 :created [model]
                                 :updated [model]}
                        :users s/Any
                        :groups s/Any})

(def get-response-body {:entitlement_group {:id s/Uuid
                                            :name s/Str
                                            :is_verification_required s/Bool}
                        :models [get-model]
                        :users s/Any
                        :groups s/Any})
