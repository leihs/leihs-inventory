(ns leihs.inventory.server.resources.pool.buildings.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.buildings.main :as buildings]
   [leihs.inventory.server.resources.pool.buildings.types :refer [response-body]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/buildings/"
   {:get {:summary (fe "")
          :accept "application/json"
          :coercion reitit.coercion.schema/coercion
          :swagger {:produces ["application/json"]}
          :produces ["application/json"]
          :parameters {:path {:pool_id s/Uuid}}
          :handler buildings/index-resources
          :responses {200 {:description "OK"
                           :body [response-body]}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}}])
