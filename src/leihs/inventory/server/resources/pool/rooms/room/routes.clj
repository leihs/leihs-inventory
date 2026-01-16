(ns leihs.inventory.server.resources.pool.rooms.room.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.pool.rooms.room.main :as room]
   [leihs.inventory.server.resources.pool.rooms.types :refer [get-response]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/rooms/:room_id"
   {:get {:accept "application/json"
          :coercion reitit.coercion.schema/coercion
          :swagger {:produces ["application/json"]}
          :produces ["application/json"]
          :parameters {:path {:room_id s/Uuid}}
          :handler room/get-resource
          :responses {200 {:description "OK"
                           :body get-response}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}}])
