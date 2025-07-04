(ns leihs.inventory.server.resources.dev.routes
  (:require
   [clojure.set]
   [leihs.core.auth.session :refer [wrap-authenticate]]
   [leihs.core.status :as status]
   [leihs.inventory.server.constants :refer [HIDE_BASIC_ENDPOINTS APPLY_DEV_ENDPOINTS]]
   [leihs.inventory.server.constants :refer [fe]]
   ;[leihs.inventory.server.resources.auth.auth-routes :refer [
   ;                                                           update-role-handler
   ;                                                           ]]
   [leihs.inventory.server.resources.dev.main :refer [run-get-views
                                                      search-in-tables
                                                      update-role-handler
                                                      update-and-fetch-accounts]]
   [leihs.inventory.server.resources.utils.flag :as i]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware wrap-is-admin! wrap-authenticate!]]
   [leihs.inventory.server.resources.utils.middleware :refer [wrap-is-admin!]]
   [leihs.inventory.server.utils.auth.inventory-auth :refer [wrap-check-authenticated-admin]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(def update-role-response {:role-before s/Str
                           :role-after s/Str
                           :inventory_pool_id s/Uuid
                           :count-of-direct-access-right-should-be-one s/Int
                           (s/optional-key :update-result) s/Any})

(defn get-dev-routes []

  [""

    ["/dev"
    {:swagger {:tags [""]}
     :no-doc (not APPLY_DEV_ENDPOINTS)}

    ["update-role"
     {:put {:summary (fe "Update direct-user-role")
            :accept "application/json"
            :description "- default pool-id: 8bd16d45-056d-5590-bc7f-12849f034351"
            :parameters {:query {:role (s/enum "inventory_manager" "lending_manager" "group_manager" "customer")
                                 (s/optional-key :pool_id) s/Uuid}}
            :coercion reitit.coercion.schema/coercion
            :middleware [wrap-is-admin!]
            :handler update-role-handler
            :responses {200 {:description "OK" :body update-role-response}
                        409 {:description "Conflict" :body update-role-response}
                        500 {:description "Internal Server Error"}}}}]


    ["/update-accounts" {:put {:summary "Overwrite pw for accounts with various roles OR is_admin"
                               :description "Fetch one account of each variant of:
- role: inventory_manager, lending_manager, group_manager, customer\n
- is_admin: true\n
- inventory_pool_id=8bd16d45-056d-5590-bc7f-12849f034351\n
- is_system_admin: true\n\n
.. and set password"
                               :accept "application/json"
                               :middleware [wrap-check-authenticated-admin]
                               :coercion reitit.coercion.schema/coercion
                               :parameters {:query {(s/optional-key :type) (s/enum "min" "all")}}
                               :handler update-and-fetch-accounts
                               :responses {200 {:description "OK"
                                                :body s/Any}
                                           404 {:description "Not Found"}
                                           500 {:description "Internal Server Error"}}}}]

    ["/usage" {:get {:summary "Used to determine appearance of uuid in tables"
                     :accept "application/json"
                     :middleware [wrap-check-authenticated-admin]
                     :coercion reitit.coercion.schema/coercion
                     :parameters {:query {(s/optional-key :id) s/Str
                                          (s/optional-key :columns) [s/Str]}}
                     :handler search-in-tables
                     :responses {200 {:description "OK"
                                      :body s/Any}
                                 404 {:description "Not Found"}
                                 500 {:description "Internal Server Error"}}}}]]])
