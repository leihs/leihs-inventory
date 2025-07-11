(ns leihs.inventory.server.resources.pool.buildings.building.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.buildings.building.main :refer [get-resource]]
   [leihs.inventory.server.resources.pool.buildings.types :refer [response-body]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn get-buildings-single-routes []
  ["/:pool_id"
   {:swagger {:tags [""]}}

   ["/buildings/"

    [":building_id"
     {:get {:summary (fe "")
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware]
            :swagger {:produces ["application/json"]}
            :parameters {:path {:pool_id s/Uuid
                                :building_id s/Uuid}}
            :handler get-resource
            :responses {200 {:description "OK"
                             :body response-body}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]]])
