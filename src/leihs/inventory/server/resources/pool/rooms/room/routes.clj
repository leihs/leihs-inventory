(ns leihs.inventory.server.resources.pool.rooms.room.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.rooms.room.main :refer [ get-rooms-handler]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn get-rooms-single-routes []
  ["/:pool_id"
   {:swagger {:tags [""]}}

   ["/rooms/"

    [":room_id"
     {:get {:summary (fe "")
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware]
            :swagger {:produces ["application/json"]}
            :parameters {:path {:room_id s/Uuid}}
            :handler get-rooms-handler
            :responses {200 {:description "OK"
                             :body [{:building_id s/Uuid
                                     :description (s/maybe s/Str)
                                     :id s/Uuid
                                     :name s/Str
                                     :general s/Bool}]}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]

]])
