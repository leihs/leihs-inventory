(ns leihs.inventory.server.resources.pool.entitlement-groups.types
  (:require
   [leihs.inventory.server.resources.types :refer [pagination]]
   [schema.core :as s]))

(def PosInt
  (s/constrained s/Int pos? 'positive-integer))

(def get-model {:id s/Uuid
                :entitlement_id s/Uuid
                :product (s/maybe s/Str)
                :name (s/maybe s/Str)
                :version (s/maybe s/Str)
                :quantity s/Int
                :available_count s/Int
                (s/optional-key :items_count) s/Int
                :is_quantity_ok s/Bool

                (s/optional-key :cover_image_id) (s/maybe s/Uuid)
                (s/optional-key :url) (s/maybe s/Str)
                (s/optional-key :content_type) (s/maybe s/Str)
                (s/optional-key :image_id) (s/maybe s/Uuid)})

(def user {:id s/Uuid
           :user_id s/Uuid
           :entitlement_group_id s/Uuid})

(def group {:id s/Uuid
            :group_id s/Uuid
            :entitlement_group_id s/Uuid
            :created_at s/Any
            :updated_at s/Any})

(def post-response-body {:id s/Uuid
                         :name s/Str
                         :inventory_pool_id s/Uuid
                         :is_verification_required s/Bool
                         :created_at s/Any
                         :updated_at s/Any
                         :models [get-model]
                         :users {:deleted [user]
                                 :created [user]}
                         :groups {:deleted [group]
                                  :created [group]}})

(def get-response {:id s/Uuid
                   :name s/Str
                   :inventory_pool_id s/Uuid
                   :is_verification_required s/Bool
                   :is_quantity_ok s/Bool
                   :created_at s/Any
                   :updated_at s/Any
                   :number_of_models s/Int
                   :number_of_groups s/Int
                   :number_of_direct_users s/Int
                   :number_of_users s/Int
                   :number_of_allocations s/Int})

(def get-response-body
  (s/->Either [[get-response] {:data [get-response] :pagination pagination}]))
