(ns leihs.inventory.server.resources.entitlements.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.entitlements.main :refer [get-entitlement-groups-of-pool-handler]]
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

(defn get-entitlements-routes []
  ["/:pool_id"
   {:swagger {:conflicting true
              :tags ["Entitlements"] :security []}}

   ["/entitlement-groups/:id"
    {:get {:conflicting true
           :summary "OK | a.k.a 'Anspruchsgruppen'"
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-middleware]
           :swagger {:produces ["application/json"]}
           :parameters {:path {:pool_id s/Uuid :id s/Uuid}}
           :handler get-entitlement-groups-of-pool-handler
           :responses {200 {:description "OK"
                            :body s/Any}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]

   ["/entitlement-groups"
    {:get {:conflicting true
           :summary "OK | a.k.a 'Anspruchsgruppen'"
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-middleware]
           :swagger {:produces ["application/json"]}
           :parameters {:path {:pool_id s/Uuid}}
           :handler get-entitlement-groups-of-pool-handler
           :responses {200 {:description "OK"
                            :body s/Any}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]])
