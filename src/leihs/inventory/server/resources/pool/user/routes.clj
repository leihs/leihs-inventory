(ns leihs.inventory.server.resources.pool.user.routes
  (:require
   [clojure.set]

   [leihs.inventory.server.resources.pool.user.types :refer :all]

   [leihs.inventory.server.resources.pool.user.main :refer [
                                                            get-pools-of-user-handler get-pools-access-rights-of-user-handler
                                                            ]]
   [leihs.inventory.server.resources.utils.flag :as i]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware wrap-is-admin! wrap-authenticate!]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

;(s/defschema manage-nav-item-schema
;  {:name s/Str
;   :url s/Str
;   :href s/Str})
;
;(s/defschema navigation-schema
;  {:borrow_url (s/maybe s/Str)
;   :admin_url (s/maybe s/Str)
;   :procure_url (s/maybe s/Str)
;   :manage_nav_items [manage-nav-item-schema]
;   :documentation_url (s/maybe s/Str)})
;
;(s/defschema inventory-pool-schema
;  {:id s/Uuid
;   :name s/Str})
;
;(s/defschema language-schema
;  {:name s/Str
;   :locale s/Str
;   :default s/Bool
;   :active s/Bool})
;
;(s/defschema profile-response-schema
;  {:navigation navigation-schema
;   :available_inventory_pools [inventory-pool-schema]
;   :user_details s/Any
;   :languages [language-schema]})
;
;(def schema-min
;  {:id s/Uuid
;   :name s/Str
;   (s/optional-key :description) (s/maybe s/Str)})

(defn get-user-routes []
  ["/"
   {:swagger {:conflicting true
              :tags ["User"]}}

   ["pools"

    ; TODO: move to DEV?
    [""
     {:get {:conflicting true
            :summary (i/session "Get pools of the authenticated user.")
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [wrap-authenticate! accept-json-middleware]
            :swagger {:produces ["application/json"]}
            :handler get-pools-of-user-handler
            :responses {200 {:description "OK"
                             ;:body [schema-min] ;; FIXME
                             }
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]

    ["-by-access-right"
     {:get {:conflicting true
            :summary (i/session "Get pools-access for menu of the authenticated user. [fe]")
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [wrap-authenticate! accept-json-middleware]
            :swagger {:produces ["application/json"]}
            :parameters {:query {(s/optional-key :min) s/Bool
                                 (s/optional-key :access_rights) (s/enum "all" "direct_access_rights" "group_access_rights")}}
            :handler get-pools-access-rights-of-user-handler
            :responses {200 {:description "OK"
                             :body [response-pbar]}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]



    ]



   ])

