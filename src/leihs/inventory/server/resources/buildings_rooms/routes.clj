(ns leihs.inventory.server.resources.buildings_rooms.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.buildings-rooms.main :refer [get-buildings-handler get-rooms-handler]]
   [leihs.inventory.server.resources.models.main :refer [get-models-handler
                                                         create-model-handler
                                                         update-model-handler
                                                         delete-model-handler]]
   [leihs.inventory.server.resources.models.models-by-pool :refer [get-models-of-pool-handler
                                                                   create-model-handler-by-pool
                                                                   get-models-of-pool-handler
                                                                   update-model-handler-by-pool
                                                                   delete-model-handler-by-pool]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn get-buildings-rooms-routes []
  [""
   {:swagger {:conflicting true
              :tags ["Buildings / Rooms"]}}

   ;["/buildings"
   ;
   ; ["/:building_id"
   ;  {:get {:conflicting true
   ;         :summary "Get building by id [v0]"
   ;         :accept "application/json"
   ;         :coercion reitit.coercion.schema/coercion
   ;         :middleware [accept-json-middleware]
   ;         :swagger {:produces ["application/json"]}
   ;         :parameters {:path {:building_id s/Uuid}}
   ;         :handler get-buildings-handler
   ;         :responses {200 {:description "OK"
   ;                          :body [{:id s/Uuid
   ;                                  :name s/Str
   ;                                  :code (s/maybe s/Str)}]}
   ;                     404 {:description "Not Found"}
   ;                     500 {:description "Internal Server Error"}}}}]
   ;
   ; [""
   ;  {:get {:conflicting true
   ;         :summary "Get buildings [v0]"
   ;         :accept "application/json"
   ;         :coercion reitit.coercion.schema/coercion
   ;         :middleware [accept-json-middleware]
   ;         :swagger {:produces ["application/json"]}
   ;         :handler get-buildings-handler
   ;         :responses {200 {:description "OK"
   ;                          :body [{:id s/Uuid
   ;                                  :name s/Str
   ;                                  :code (s/maybe s/Str)}]}
   ;                     404 {:description "Not Found"}
   ;                     500 {:description "Internal Server Error"}}}}]]

   ;["/rooms"
   ;
   ; ["/:room_id"
   ;  {:get {:conflicting true
   ;         :summary "Get room by id [v0]"
   ;         :accept "application/json"
   ;         :coercion reitit.coercion.schema/coercion
   ;         :middleware [accept-json-middleware]
   ;         :swagger {:produces ["application/json"]}
   ;         :parameters {:path {:room_id s/Uuid}}
   ;         :handler get-rooms-handler
   ;         :responses {200 {:description "OK"
   ;                          :body [{:building_id s/Uuid
   ;                                  :description (s/maybe s/Str)
   ;                                  :id s/Uuid
   ;                                  :name s/Str
   ;                                  :general s/Bool}]}
   ;                     404 {:description "Not Found"}
   ;                     500 {:description "Internal Server Error"}}}}]
   ;
   ; [""
   ;  {:get {:conflicting true
   ;         :summary "Get rooms of building [v0]"
   ;         :accept "application/json"
   ;         :coercion reitit.coercion.schema/coercion
   ;         :middleware [accept-json-middleware]
   ;         :swagger {:produces ["application/json"]}
   ;         :parameters {:query {(s/optional-key :building_id) s/Uuid}}
   ;         :handler get-rooms-handler
   ;         :responses {200 {:description "OK"
   ;                          :body [{:building_id s/Uuid
   ;                                  :description (s/maybe s/Str)
   ;                                  :id s/Uuid
   ;                                  :name s/Str
   ;                                  :general s/Bool}]}
   ;                     404 {:description "Not Found"}
   ;                     500 {:description "Internal Server Error"}}}}]]

   ])
