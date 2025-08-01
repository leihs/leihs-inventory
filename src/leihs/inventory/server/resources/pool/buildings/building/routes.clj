(ns leihs.inventory.server.resources.pool.buildings.building.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.buildings.building.main :as building]
   [leihs.inventory.server.resources.pool.buildings.types :refer [response-body]]
   [leihs.inventory.server.utils.middleware :refer [accept-json-middleware]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/buildings/:building_id"
   {:get {:summary (fe "")
          :accept "application/json"
          :coercion reitit.coercion.schema/coercion
          :middleware [accept-json-middleware]
          :swagger {:produces ["application/json"]}
          :parameters {:path {:pool_id s/Uuid
                              :building_id s/Uuid}}
          :handler building/get-resource
          :responses {200 {:description "OK"
                           :body response-body}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}}])
