(ns leihs.inventory.server.resources.pool.entitlement-groups.types
  (:require
   [leihs.inventory.server.resources.types :refer [pagination]]
   [schema.core :as s]))

(def PosInt
  (s/constrained s/Int pos? 'positive-integer))

(def post-user {:id s/Uuid
                :firstname (s/maybe s/Str)
                :lastname (s/maybe s/Str)
                :email (s/maybe s/Str)
                (s/optional-key :type) (s/maybe s/Str)
                :searchable s/Str})

(def post-group {:id s/Uuid
                 :name s/Str
                 :searchable s/Str})

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

(def post-response-body {:id s/Uuid
                         :name s/Str
                         :inventory_pool_id s/Uuid
                         :is_verification_required s/Bool
                         :created_at s/Any
                         :updated_at s/Any
                         :models [get-model]
                         :users [post-user]
                         :groups [post-group]})

(def get-response {:id s/Uuid
                   :name s/Str
                   :inventory_pool_id s/Uuid
                   :is_verification_required s/Bool
                   :created_at s/Any
                   :updated_at s/Any

                   (s/optional-key :is_quantity_ok) s/Bool
                   (s/optional-key :number_of_models) s/Int
                   (s/optional-key :number_of_groups) s/Int
                   (s/optional-key :number_of_direct_users) s/Int
                   (s/optional-key :number_of_users) s/Int
                   (s/optional-key :number_of_allocations) (s/maybe s/Int)})

(def get-response-body
  (s/->Either [[get-response] {:data [get-response] :pagination pagination}]))
