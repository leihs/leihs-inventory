(ns leihs.inventory.server.resources.pool.accessories.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.pool.accessories.main :refer [get-accessories-of-pool-handler get-accessories-handler]]
   ;[leihs.inventory.server.resources.pool.models._main :refer [get-models-handler
   ;                                                      create-model-handler
   ;                                                      update-model-handler
   ;                                                      delete-model-handler]]
   ;[leihs.inventory.server.resources.pool.models.models-by-pool :refer [get-models-of-pool-handler
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

(defn get-accessories-routes []

  ["/"
   {:swagger {:conflicting true
              :tags []}}

   [":pool_id/models/:model_id"
    {:swagger {:conflicting true
               :tags []}}

    ;["/accessories"
    ; {:get {:conflicting true
    ;        :accept "application/json"
    ;        :coercion reitit.coercion.schema/coercion
    ;        :middleware [accept-json-middleware]
    ;        :swagger {:produces ["application/json"]}
    ;        :parameters {:path {:pool_id s/Uuid :model_id s/Uuid}}
    ;        :handler get-accessories-of-pool-handler
    ;        :responses {200 {:description "OK"
    ;                         :body s/Any}
    ;                    404 {:description "Not Found"}
    ;                    500 {:description "Internal Server Error"}}}}]

    ["/accessories/:id"
     {:get {:conflicting true
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware]
            :swagger {:produces ["application/json"]}
            :parameters {:path {:pool_id s/Uuid :id s/Uuid}}
            :handler get-accessories-of-pool-handler
            :responses {200 {:description "OK"
                             :body s/Any}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]

    ["/accessories"
     {:get {:conflicting true
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware]
            :swagger {:produces ["application/json"]}
            :parameters {:path {:pool_id s/Uuid}}
            :handler get-accessories-of-pool-handler
            :responses {200 {:description "OK"
                             :body s/Any}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]]

   ;["accessories/:id"
   ; {:get {:conflicting true
   ;        :accept "application/json"
   ;        :coercion reitit.coercion.schema/coercion
   ;        :middleware [accept-json-middleware]
   ;        :swagger {:produces ["application/json"]}
   ;        :parameters {:path {:id s/Uuid}}
   ;        :handler get-accessories-handler
   ;        :responses {200 {:description "OK"
   ;                         :body s/Any}
   ;                    404 {:description "Not Found"}
   ;                    500 {:description "Internal Server Error"}}}}]
   ;
   ;["accessories/"
   ; {:get {:conflicting true
   ;        :accept "application/json"
   ;        :coercion reitit.coercion.schema/coercion
   ;        :middleware [accept-json-middleware]
   ;        :swagger {:produces ["application/json"]}
   ;      ;:parameters {:path {:pool_id s/Uuid}}
   ;        :handler get-accessories-handler
   ;        :responses {200 {:description "OK"
   ;                         :body s/Any}
   ;                    404 {:description "Not Found"}
   ;                    500 {:description "Internal Server Error"}}}}]

   ])
