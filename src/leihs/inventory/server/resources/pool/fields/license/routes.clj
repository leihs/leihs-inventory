(ns leihs.inventory.server.resources.pool.fields.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.auth.session :as session]
   [leihs.inventory.server.resources.pool.fields.license.main :refer [get-form-fields-auto-new-pagination-handler]]

   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn get-fields-license-routes []
  [""

   ["/license"
    {:swagger {:conflicting true
               :tags ["Fields"]}}

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
                           500 {:description "Internal Server Error"}}}}]]])
