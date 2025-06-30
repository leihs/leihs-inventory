(ns leihs.inventory.server.resources.profile.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.profile.main :refer [get-user-profile]]
   [leihs.inventory.server.resources.utils.flag :as i]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware wrap-authenticate!]]
   [leihs.inventory.server.utils.response_helper :as rh]
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

(s/defschema profile-response-schema
  {:navigation navigation-schema
   :available_inventory_pools [inventory-pool-schema]
   :user_details s/Any
   :languages [language-schema]})

(def schema-min
  {:id s/Uuid
   :name s/Str
   (s/optional-key :description) (s/maybe s/Str)})

(defn get-profile-routes []
  ["/"
   {:swagger {:conflicting true
              :tags ["User"]}}


   ["profile"
    {:get {:conflicting true
           :accept "application/json"
           :summary (i/session "Get details of the authenticated user. [fe]")
           :description "Uses /inventory/pools-by-access-right for the pools."
           :coercion reitit.coercion.schema/coercion
           :middleware [wrap-authenticate! accept-json-middleware]
           :swagger {:produces ["application/json"]}
           :handler get-user-profile
           :responses {200 {:description "OK"
                            :body profile-response-schema}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]])

