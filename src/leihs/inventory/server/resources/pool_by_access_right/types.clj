(ns leihs.inventory.server.resources.pool-by-access-right.types
  (:require
   [clojure.set]
   [schema.core :as s]))

(s/defschema response-pbar {:id s/Uuid
                            :name s/Str
                            (s/optional-key :inventory_pool_id) s/Uuid
                            (s/optional-key :user_id) s/Uuid
                            (s/optional-key :group_access_right_id) (s/maybe s/Uuid)
                            (s/optional-key :direct_access_right_id) (s/maybe s/Uuid)
                            (s/optional-key :role) s/Str
                            (s/optional-key :created_at) s/Any
                            (s/optional-key :updated_at) s/Any
                            (s/optional-key :origin_table) s/Str
                            (s/optional-key :is_active) s/Bool})

(s/defschema manage-nav-item-schema
  {:name s/Str
   :url s/Str
   :href s/Str})

(s/defschema navigation-schema
  {:borrow_url (s/maybe s/Str)
   :admin_url (s/maybe s/Str)
   :procure_url (s/maybe s/Str)
   :manage_nav_items [manage-nav-item-schema]
   :documentation_url (s/maybe s/Str)})

(s/defschema inventory-pool-schema
  {:id s/Uuid
   :name s/Str})

(s/defschema language-schema
  {:name s/Str
   :locale s/Str
   :default s/Bool
   :active s/Bool})

(s/defschema profile-response-schema
  {:navigation navigation-schema
   :available_inventory_pools [inventory-pool-schema]
   :user_details s/Any
   :languages [language-schema]})

(def schema-min
  {:id s/Uuid
   :name s/Str
   (s/optional-key :description) (s/maybe s/Str)})


