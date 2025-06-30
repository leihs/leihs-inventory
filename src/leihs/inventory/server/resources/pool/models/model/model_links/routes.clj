(ns leihs.inventory.server.resources.pool.models.model.model-links.routes
  (:require
   [clojure.spec.alpha :as sa]
   [clojure.string :as str]
   [leihs.inventory.server.resources.pool.models.coercion :as mc]

   [leihs.inventory.server.resources.pool.models.model.create-model-form :refer [create-model-handler-by-pool-model-json]]

   ;[leihs.inventory.server.resources.pool.models.main :refer [create-model-handler-by-pool get-models-of-pool-with-pagination-handler]]

   ;[leihs.inventory.server.resources.pool.models.model.properties.main :refer [get-properties-with-pagination-handler]]
   [leihs.inventory.server.resources.pool.models.model.model-links.main :refer [get-model-links-with-pagination-handler]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.auth.role-auth :refer [permission-by-role-and-pool]]
   [leihs.inventory.server.utils.auth.roles :as roles]
   [leihs.inventory.server.utils.coercion.core :refer [Date]]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn get-models-single-model-links-route []
  ["/:pool_id"

   {:swagger {:conflicting true
              :tags []}}

;; Routes for /inventory/<pool-id>/*
   ;; TODO: should be? ["/models/list"
   ["/models/:model_id"

    ;["/properties"
    ; ["" {:get {:accept "application/json"
    ;            :coercion reitit.coercion.schema/coercion
    ;            :middleware [accept-json-middleware]
    ;            :swagger {:produces ["application/json"]}
    ;            :parameters {:path {:pool_id s/Uuid
    ;                                :model_id s/Uuid}}
    ;            :handler get-properties-with-pagination-handler
    ;            :responses {200 {:description "OK"
    ;                             ;:body (s/->Either [s/Any schema])}
    ;                             :body s/Any}
    ;
    ;                        404 {:description "Not Found"}
    ;                        500 {:description "Internal Server Error"}}}}]
    ;
    ; ["/:property_id"
    ;  {:get {:accept "application/json"
    ;         :coercion reitit.coercion.schema/coercion
    ;         :middleware [accept-json-middleware]
    ;         :swagger {:produces ["application/json"]}
    ;         :parameters {:path {:pool_id s/Uuid
    ;                             :model_id s/Uuid
    ;                             :property_id s/Uuid}}
    ;         :handler get-properties-with-pagination-handler
    ;
    ;         :responses {200 {:description "OK"
    ;                          ;:body (s/->Either [s/Any schema])}
    ;                          :body s/Any}
    ;
    ;                     404 {:description "Not Found"}
    ;                     500 {:description "Internal Server Error"}}}}]]

    ["/model-links"
     ["" {:get {:accept "application/json"
                :coercion reitit.coercion.schema/coercion
                :middleware [accept-json-middleware]
                :swagger {:produces ["application/json"]
                          :deprecated true}
                :parameters {:path {:model_id s/Uuid}}
                :handler get-model-links-with-pagination-handler
                :responses {200 {:description "OK"
                                 :body s/Any}
                            404 {:description "Not Found"}
                            500 {:description "Internal Server Error"}}}}]

     ["/:model_link_id"
      {:get {:accept "application/json"
             :coercion reitit.coercion.schema/coercion
             :middleware [accept-json-middleware]
             :swagger {:produces ["application/json"]
                       :deprecated true}
             :parameters {:path {:model_id s/Uuid
                                 :model_link_id s/Uuid}}
             :handler get-model-links-with-pagination-handler
             :responses {200 {:description "OK"
                              :body s/Any}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}]]]])
