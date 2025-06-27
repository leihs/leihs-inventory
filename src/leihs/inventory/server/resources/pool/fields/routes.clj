(ns leihs.inventory.server.resources.pool.fields.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.auth.session :as session]
   [leihs.inventory.server.resources.pool.fields.main :refer [

                                                         get-form-fields-auto-pagination-handler]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn get-fields-routes []
  [""


   ["/fields"
    {:swagger {:conflicting true
               :tags ["Fields"]}}

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




    ]

]
)
