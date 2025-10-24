(ns leihs.inventory.server.resources.profile.types
  (:require
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

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

(def profile-response-schema
  {:navigation navigation-schema
   :available_inventory_pools [inventory-pool-schema]
   :user_details s/Any
   :languages [language-schema]})

(def profile-patch-schema
  {:language s/Str})
