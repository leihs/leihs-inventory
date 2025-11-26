(ns leihs.inventory.server.resources.pool.entitlement-groups.entitlement-group.types
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.pool.entitlement-groups.types :refer [get-model post-user post-group]]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(def delete-response-body {:id s/Uuid
                           :name s/Str
                           :is_verification_required s/Bool
                           :inventory_pool_id s/Uuid
                           :created_at s/Any
                           :updated_at s/Any
                           :models [{:id s/Uuid
                                     :entitlement_group_id s/Uuid
                                     :model_id s/Uuid
                                     :quantity s/Int
                                     :position s/Int}]})

(def put-response-body {:id s/Uuid
                        :name s/Str
                        :is_verification_required s/Bool
                        :inventory_pool_id s/Uuid
                        :created_at s/Any
                        :updated_at s/Any
                        :models [get-model]
                        :users [post-user]
                        :groups [post-group]})

(def get-response-body {:id s/Uuid
                        :name s/Str
                        :is_verification_required s/Bool
                        :models [get-model]
                        :users [post-user]
                        :groups [post-group]})
