(ns leihs.inventory.server.resources.pool.groups.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.pool.groups.main :refer [
                                                         ;get-model-groups-of-pool-handler
                                                             get-groups-of-pool-handler
                                                             ;get-entitlement-groups-of-pool-handler
                                                             ;get-model-group-links-of-pool-handler
                                                         ]]
   ;[leihs.inventory.server.resources.models.main :refer [get-models-handler
   ;                                                      create-model-handler
   ;                                                      update-model-handler
   ;                                                      delete-model-handler]]
   ;[leihs.inventory.server.resources.models.models-by-pool :refer [get-models-of-pool-handler
   ;                                                                create-model-handler-by-pool
   ;                                                                get-models-of-pool-handler
   ;                                                                update-model-handler-by-pool
   ;                                                                delete-model-handler-by-pool]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))


(defn get-groups-routes []

  [""


   ["/:pool_id"
    {:swagger {:conflicting true
               :tags ["Groups"]}}



    ["/groups"
     ["" {:get {:conflicting true
                :accept "application/json"
                :coercion reitit.coercion.schema/coercion
                :middleware [accept-json-middleware]
                :swagger {:produces ["application/json"]}
                :parameters {:path {:pool_id s/Uuid}}
                :handler get-groups-of-pool-handler
                :responses {200 {:description "OK"
                                 :body s/Any}
                            404 {:description "Not Found"}
                            500 {:description "Internal Server Error"}}}}]

     ["/:group_id"
      {:get {:conflicting true
             :accept "application/json"
             :coercion reitit.coercion.schema/coercion
             :middleware [accept-json-middleware]
             :swagger {:produces ["application/json"]}
             :parameters {:path {:pool_id s/Uuid :group_id s/Uuid}}
             :handler get-groups-of-pool-handler
             :responses {200 {:description "OK"
                              :body s/Any}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}]]



    ]])