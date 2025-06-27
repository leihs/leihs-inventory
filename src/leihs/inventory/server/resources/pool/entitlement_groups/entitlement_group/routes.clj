(ns leihs.inventory.server.resources.pool.entitlement-groups.entitlement-group.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.pool.entitlement-groups.entitlement-group.main :refer [
                                                             get-entitlement-groups-of-pool-handler
                                                             ]]

   [leihs.inventory.server.resources.pool.entitlement-groups.types :refer [response-body]]

   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn create-description [url]
  (str "- GET " url " Accept: application/json "))

(defn get-entitlement-groups-single-routes []

  [""
   ["/:pool_id"
    {:swagger {:conflicting true
               :tags ["Categories / Model-Groups"]}}

     ["/entitlement-groups/:entitlement_group_id"
      {:get {:conflicting true
             :summary "OK | a.k.a 'Anspruchsgruppen'"
             :accept "application/json"
             :coercion reitit.coercion.schema/coercion
             :middleware [accept-json-middleware]
             :swagger {:produces ["application/json"]}
             :parameters {:path {:pool_id s/Uuid :entitlement_group_id s/Uuid}}
             :handler get-entitlement-groups-of-pool-handler
             :responses {200 {:description "OK"
                              :body [response-body]}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}]

   ]])
