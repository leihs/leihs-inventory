(ns leihs.inventory.server.resources.pool.entitlement-groups.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.pool.entitlement-groups.main :as entitlement-groups]
   [leihs.inventory.server.resources.pool.entitlement-groups.types :refer [response-body]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/entitlement-groups/"
   {:get {:accept "application/json"
          :coercion reitit.coercion.schema/coercion
          :swagger {:produces ["application/json"]}
          :parameters {:path {:pool_id s/Uuid}}
          :produces ["application/json"]
          :handler entitlement-groups/index-resources
          :responses {200 {:description "OK"
                           :body [response-body]}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}}])
