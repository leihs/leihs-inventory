(ns leihs.inventory.server.resources.pool.entitlement-groups.types
  (:require
   [leihs.inventory.server.resources.types :refer [pagination]]
   [schema.core :as s]))

(def PosInt
  (s/constrained s/Int pos? 'positive-integer))

(def model {:id s/Uuid
            :model_id s/Uuid
            :entitlement_group_id s/Uuid
            :quantity PosInt
            :position s/Int})

(def get-model {:id s/Uuid
                :model_id s/Uuid
                :name s/Str
                :quantity PosInt
                :available_count s/Int
                :is_quantity_ok s/Bool})

(def user {:id s/Uuid
           :user_id s/Uuid
           :entitlement_group_id s/Uuid})

(def group {:id s/Uuid
            :group_id s/Uuid
            :entitlement_group_id s/Uuid
            :created_at s/Any
            :updated_at s/Any})

(def post-response-body {:entitlement_group {:id s/Uuid
                                             :name s/Str
                                             :inventory_pool_id s/Uuid
                                             :is_verification_required s/Bool
                                             :created_at s/Any
                                             :updated_at s/Any}
                         :models [model]
                         :users {:deleted [user]
                                 :created [user]}
                         :groups {:deleted [group]
                                  :created [group]}})

(def get-response {:id s/Uuid
                   :name s/Str
                   :inventory_pool_id s/Uuid
                   :is_verification_required s/Bool
                   :created_at s/Any
                   :updated_at s/Any

                   :number_of_models s/Int
                   :number_of_groups s/Int
                   :number_of_direct_users s/Int
                   :number_of_users s/Int})

(def get-response-body
  (s/->Either [[get-response] {:data [get-response] :pagination pagination}]))
