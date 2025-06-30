(ns leihs.inventory.server.resources.pool.fields.field.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.auth.session :as session]
   [leihs.inventory.server.resources.pool.fields.field.main :refer [get-form-fields-handler]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn get-fields-single-routes []
  ["/:pool_id"

   ["/fields/:field_id"
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
                       500 {:description "Internal Server Error"}}}}]])
