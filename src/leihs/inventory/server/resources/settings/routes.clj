(ns leihs.inventory.server.resources.settings.routes
  (:require
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.settings.main :as settings]
   [leihs.inventory.server.resources.settings.types :refer [response-schema]]
   [leihs.inventory.server.utils.middleware :refer [accept-json-middleware
                                                    wrap-authenticate!]]
   [reitit.coercion.schema]))

(defn routes []
  ["settings/"
   {:get {:accept "application/json"
          :summary (fe "Get settings")
          :description "Settings are defined in admin app"
          :coercion reitit.coercion.schema/coercion
          :middleware [wrap-authenticate! accept-json-middleware]
          :swagger {:produces ["application/json"]}
          :handler settings/get-resource
          :responses {200 {:description "OK"
                           :body response-schema}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}}])
