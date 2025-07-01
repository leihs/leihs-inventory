(ns leihs.inventory.server.resources.pool.entitlement-groups.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.pool.entitlement-groups.main :refer [get-entitlement-groups-of-pool-handler]]
   [leihs.inventory.server.resources.pool.entitlement-groups.types :refer [response-body]]

   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn create-description [url]
  (str "- GET " url " Accept: application/json "))

(defn get-entitlement-groups-routes []

  [""
   ["/:pool_id"
    {:swagger {:conflicting true
               :tags []}}

    ["/entitlement-groups"
     ["/" {:get {:conflicting true
                :summary "a.k.a 'Anspruchsgruppen' [fe]"
                :description (create-description "https://staging.leihs.zhdk.ch/manage/8bd16d45-056d-5590-bc7f-12849f034351/groups")
                :accept "application/json"
                :coercion reitit.coercion.schema/coercion
                :middleware [accept-json-middleware]
                :swagger {:produces ["application/json"]}
                :parameters {:path {:pool_id s/Uuid}}
                :handler get-entitlement-groups-of-pool-handler
                :responses {200 {:description "OK"
                                 :body [response-body]}
                            404 {:description "Not Found"}
                            500 {:description "Internal Server Error"}}}}]]]])
