(ns leihs.inventory.server.resources.dev.routes
  (:require
   [clojure.set]
   [leihs.core.auth.session :refer [wrap-authenticate]]
   [leihs.inventory.server.resources.dev.main :refer [run-get-views
                                                      search-in-tables
                                                      update-and-fetch-accounts]]
   [leihs.inventory.server.utils.auth.inventory-auth :refer [wrap-check-authenticated-admin]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn get-dev-routes []

  [""

   ["/dev"
    {:swagger {:conflicting true
               :tags ["Dev"]}}
    ["/update-accounts" {:put {:conflicting true
                               :summary "Overwrite pw for accounts with various roles OR is_admin"
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

    ["/usage" {:get {:conflicting true
                     :summary "Used to determine appearance of uuid in tables"
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
