(ns leihs.inventory.server.resources.pool.models.model.entitlements.routes
  (:require
   [clojure.spec.alpha :as sa]
   [clojure.string :as str]
   [leihs.inventory.server.resources.pool.models.coercion :as mc]

   [leihs.inventory.server.resources.pool.models.model.entitlements.main :refer [get-entitlements-with-pagination-handler]]

   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.auth.role-auth :refer [permission-by-role-and-pool]]
   [leihs.inventory.server.utils.auth.roles :as roles]
   [leihs.inventory.server.utils.coercion.core :refer [Date]]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn get-models-single-entitlements-route []
  ["/:pool_id"

   {:swagger {:conflicting true
              :tags ["Models by pool"]}}

;; Routes for /inventory/<pool-id>/*
   ;; TODO: should be? ["/models/list"
   ["/models/:model_id"

    ["/entitlements"
     ["" {:get {:accept "application/json"
                :summary "(T)"
                :coercion reitit.coercion.schema/coercion
                :middleware [accept-json-middleware]
                :swagger {:produces ["application/json"]}
                :parameters {:path {:pool_id s/Uuid
                                    :model_id s/Uuid}}
                :handler get-entitlements-with-pagination-handler
                :responses {200 {:description "OK"
                                 ;:body (s/->Either [s/Any schema])}
                                 :body s/Any}

                            404 {:description "Not Found"}
                            500 {:description "Internal Server Error"}}}}]

     ["/:entitlement_id"
      {:get {:accept "application/json"
             :coercion reitit.coercion.schema/coercion
             :middleware [accept-json-middleware]
             :swagger {:produces ["application/json"]}
             :parameters {:path {:pool_id s/Uuid
                                 :model_id s/Uuid
                                 :entitlement_id s/Uuid}}
             :handler get-entitlements-with-pagination-handler

             :responses {200 {:description "OK"
                              ;:body (s/->Either [s/Any schema])}
                              :body s/Any}

                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}]]]])
