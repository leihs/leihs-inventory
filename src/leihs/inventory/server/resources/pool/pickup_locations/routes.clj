(ns leihs.inventory.server.resources.pool.pickup-locations.routes
  (:require
   [leihs.inventory.server.resources.pool.pickup-locations.main :as pickup-locations]
   [reitit.coercion.schema]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/pickup-locations/"
   {:get {:accept "application/json"
          :coercion reitit.coercion.schema/coercion
          :produces ["application/json"]
          :swagger {:produces ["application/json"]}
          :summary "Whether the pool has alternative pickup locations"
          :parameters {:path {:pool_id s/Uuid}}
          :handler pickup-locations/index-resources
          :responses {200 {:description "OK"
                           :body {:has_pickup_locations s/Bool}}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}}])
