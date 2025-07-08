(ns leihs.inventory.server.resources.profile.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.profile.main :refer [get-resource]]
   [leihs.inventory.server.resources.profile.types :refer [profile-response-schema]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware wrap-authenticate!]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn get-profile-routes []
  ["/"
   {:swagger {:tags [""]}}

   ["profile/"
    {:get {:accept "application/json"
           :summary (fe "Get details of the authenticated user")
           :description "Uses /inventory/pools-by-access-right for the pools."
           :coercion reitit.coercion.schema/coercion
           :middleware [wrap-authenticate! accept-json-middleware]
           :swagger {:produces ["application/json"]}
           :handler get-resource
           :responses {200 {:description "OK"
                            :body profile-response-schema}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]])

