(ns leihs.inventory.server.resources.profile.routes
  (:require
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.profile.main :as profile]
   [leihs.inventory.server.resources.profile.types :as type]
   [reitit.coercion.schema]))

(defn routes []
  ["/profile/"
   {:get {:accept "application/json"
          :summary (fe "Get details of the authenticated user")
          :description "Uses /inventory/pools-by-access-right for the pools."
          :coercion reitit.coercion.schema/coercion
          :produces ["application/json"]
          :handler profile/get-resource
          :responses {200 {:description "OK"
                           :body type/profile-response-schema}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}

    :patch {:accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :swagger {:produces ["application/json"]}
            :parameters {:body type/profile-patch-schema}
            :handler profile/patch-resource
            :responses {200 {:description "OK"
                             :body type/profile-patch-schema}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}])
