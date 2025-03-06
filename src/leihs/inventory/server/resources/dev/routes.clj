(ns leihs.inventory.server.resources.dev.routes
  (:require
   [clojure.set]
   [leihs.core.auth.session :refer [wrap-authenticate]]
   [leihs.inventory.server.resources.dev.main :refer [run-get-views
                                                      ;run-search
                                                      ;search-in-views
                                                      search-in-tables
                                                      update-and-fetch-accounts
                                                      ;search-uuid-in-db
                                                      ]]
   [leihs.inventory.server.utils.auth.inventory-auth :refer [wrap-check-authenticated-admin]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn get-dev-routes []

  [""

   ["/dev"
    {:swagger {:conflicting true
               :tags ["Dev"] :security []}}

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
                               :swagger {:security [{:basicAuth []}] :produces ["application/json"]}
                               :parameters {:query {(s/optional-key :type) (s/enum "min" "all")}}
                               :handler update-and-fetch-accounts
                               :responses {200 {:description "OK"
                                                :body s/Any}
                                           404 {:description "Not Found"}
                                           500 {:description "Internal Server Error"}}}}]


    ["/used-by" {:get {:conflicting true
                               :accept "application/json"
                               :middleware [wrap-check-authenticated-admin]
                               :coercion reitit.coercion.schema/coercion
                               :swagger {:security [{:basicAuth []}] :produces ["application/json"]}
                       ;:swagger {:security [{:basicAuth []}] :produces ["text/plain"]}

                       :handler search-in-tables
                       ;:handler search-in-views
                       ;:handler run-get-views
                       ;:handler run-search
                               :responses {200 {:description "OK"
                                                :body s/Any}
                                           404 {:description "Not Found"}
                                           500 {:description "Internal Server Error"}}}}]


    ]])
