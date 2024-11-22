(ns leihs.inventory.server.resources.fields.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.auth.session :as session]
   [leihs.inventory.server.resources.fields.form-fields :refer [get-form-fields-auto-new-pagination-handler]]
   [leihs.inventory.server.resources.fields.main :refer [get-form-fields-handler
                                                         get-form-fields-auto-pagination-handler]]
   [leihs.inventory.server.resources.fields.search :refer [get-search-with-pagination-handler]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn get-fields-routes []
  [""

   ["/fields-for-form"
    {:swagger {:conflicting true
               :tags ["Form fields"] :security []}}
    ["" {:get {:conflicting true
               :summary "(admin-endpoint?)"
               :description (str "<ul>"
                                 "<li>Form: https://staging.leihs.zhdk.ch/manage/8bd16d45-056d-5590-bc7f-12849f034351/fields?target_type=itemRequest</li>"
                                 "<li>ToDo: Fields by User/:pool_id?</li>"
                                 "<ul/>")
               :accept "application/json"
               :coercion reitit.coercion.schema/coercion
               :middleware [accept-json-middleware
                            ;session/wrap
                            ]
               :swagger {:produces ["application/json"]}
               :parameters {:query {(s/optional-key :page) s/Int
                                    (s/optional-key :size) s/Int
                                    (s/optional-key :role) (s/enum "inventory_manager" "lending_manager" "group_manager" "customer")
                                    (s/optional-key :owner) s/Bool
                                    (s/optional-key :type) (s/enum "license")}}
               :handler get-form-fields-auto-pagination-handler
               :responses {200 {:description "OK"
                                :body s/Any}
                           404 {:description "Not Found"}
                           500 {:description "Internal Server Error"}}}}]]

   ["/fields-license"
    {:swagger {:conflicting true
               :tags ["Form fields"] :security []}}

    ["" {:get {:conflicting true
               :summary "Used by license form"
               :description (str "<ul>"
                                 "<li>TODO: role & owner should be fetched from session</li>"
                                 "<ul/>")
               :accept "application/json"
               :coercion reitit.coercion.schema/coercion
               :middleware [accept-json-middleware
                            ;session/wrap
                            ;; TODO: add session/auth check to determine role
                            ]
               :swagger {:produces ["application/json"]}
               :parameters {:query {(s/optional-key :page) s/Int
                                    (s/optional-key :size) s/Int
                                    (s/optional-key :role) (s/enum "inventory_manager" "lending_manager" "group_manager" "customer")
                                    (s/optional-key :owner) s/Bool}}
               :handler get-form-fields-auto-new-pagination-handler
               :responses {200 {:description "OK"
                                :body s/Any}
                           404 {:description "Not Found"}
                           500 {:description "Internal Server Error"}}}}]]

   ["/fields"
    {:swagger {:conflicting true
               :tags ["Form fields"] :security []}}

    ["" {:get {:conflicting true
               :summary "(admin-endpoint?)"
               :description (str "<ul>"
                                 "<li>Form: https://staging.leihs.zhdk.ch/manage/8bd16d45-056d-5590-bc7f-12849f034351/fields?target_type=itemRequest</li>"
                                 "<li>ToDo: Fields by User/:pool_id?</li>"
                                 "<ul/>")
               :accept "application/json"
               :coercion reitit.coercion.schema/coercion
               :middleware [accept-json-middleware
                            ;session/wrap
                            ]
               :swagger {:produces ["application/json"]}
               :parameters {:query {(s/optional-key :page) s/Int
                                    (s/optional-key :size) s/Int
                                    (s/optional-key :role) (s/enum "inventory_manager" "lending_manager" "group_manager" "customer")
                                    (s/optional-key :owner) s/Bool
                                    (s/optional-key :type) (s/enum "license")}}
               :handler get-form-fields-auto-pagination-handler
               :responses {200 {:description "OK"
                                :body s/Any}
                           404 {:description "Not Found"}
                           500 {:description "Internal Server Error"}}}}]

    ["/:field_id"
     {:get {:conflicting true
            :accept "application/json"
            :description (str "<ul>"
                              "<li>properties_license_type</li>"
                              "<li>properties_activation_type</li>"
                              "<li>properties_operating_system</li>"
                              "<li>properties_installation</li>"
                              "<li>properties_maintenance_contract</li>"
                              "<li>properties_maintenance_contract</li>"
                              "<li>retired</li>"
                              "<li>retired_reason</li>"
                              "<li>is_borrowable</li>"
                              "<li>properties_reference</li>"
                              "<li>...</li>"
                              "<ul/>")
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware
                         ;session/wrap
                         ]
            :swagger {:produces ["application/json"]}
            :parameters {:path {:field_id s/Str}}
            :handler get-form-fields-handler
            :responses {200 {:description "OK"
                             :body s/Any}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]]

   ["/:pool_id/search"
    {:swagger {:conflicting true
               :tags ["Form fields"] :security []}}
    ["" {:get {:conflicting true
               :description (str "- https://test.leihs.zhdk.ch/manage/8bd16d45-056d-5590-bc7f-12849f034351/inventory.json?search_term=abc&retired=false&used=true&page=1&include_package_models=true&sort=name&order=ASC"
                                 "- TODO: add facets / fix software-query")
               :accept "application/json"
               :coercion reitit.coercion.schema/coercion
               :middleware [accept-json-middleware
                            ;session/wrap
                            ]
               :swagger {:produces ["application/json"]}
               :parameters {:path {:pool_id s/Uuid}
                            :query {(s/optional-key :type) (s/enum "Software" "Model")
                                    (s/optional-key :page) s/Int
                                    (s/optional-key :size) s/Int}}
               :handler get-search-with-pagination-handler
               :responses {200 {:description "OK"
                                :body s/Any}
                           404 {:description "Not Found"}
                           500 {:description "Internal Server Error"}}}}]]])
