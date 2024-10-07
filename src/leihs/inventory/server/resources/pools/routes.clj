(ns leihs.inventory.server.resources.pools.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.pools.main :refer [get-pools-handler]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn get-pools-routes []
  ["/"
   {:swagger {:conflicting true
              :tags ["Pool"] :security []}}

   ["pools"
    {:get {:conflicting true
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-middleware]
           :swagger {:produces ["application/json"]}
           :parameters {:query {:login s/Str}}
           :handler get-pools-handler
           :responses {200 {:description "OK"
                            ;:body (s/->Either [s/Any schema])}
                            :body s/Any}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]])
