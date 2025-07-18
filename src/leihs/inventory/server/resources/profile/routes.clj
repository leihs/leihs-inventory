(ns leihs.inventory.server.resources.profile.routes
  (:require
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.profile.main :as profile]
   [leihs.inventory.server.resources.profile.types :refer [profile-response-schema]]
   [leihs.inventory.server.utils.middleware :refer [accept-json-middleware
                                                    wrap-authenticate!]]))

(defn routes []
   ["/profile/"
    {:get {:accept "application/json"
           :summary (fe "Get details of the authenticated user")
           :description "Uses /inventory/pools-by-access-right for the pools."
           :coercion reitit.coercion.schema/coercion
           :middleware [wrap-authenticate! accept-json-middleware]
           :swagger {:produces ["application/json"]}
           :handler profile/get-resource
           :responses {200 {:description "OK"
                            :body profile-response-schema}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}])
