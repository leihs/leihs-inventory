(ns leihs.inventory.server.resources.pool.properties.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.pool.properties.main :refer [get-properties-handler]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn get-properties-routes []
  ["/"
   {:swagger {:conflicting true
              :tags ["Properties"]}}

   ["properties/"
    {:get {:conflicting true
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-middleware]
           :swagger {:produces ["application/json"]}
           :handler get-properties-handler
           :responses {200 {:description "OK"
                            :body s/Any}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]])
