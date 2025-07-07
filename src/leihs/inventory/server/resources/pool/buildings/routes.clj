(ns leihs.inventory.server.resources.pool.buildings.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.buildings.main :refer [get-buildings-handler]]
   [leihs.inventory.server.resources.pool.buildings.types :refer [response-body]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn get-buildings-routes []
  ["/:pool_id"
   {:swagger {:tags [""]}}

   ["/buildings/"

    [""
     {:get {:summary (fe "")
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware]
            :swagger {:produces ["application/json"]}
            :handler get-buildings-handler
            :responses {200 {:description "OK"
                             :body [response-body]}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]]])
