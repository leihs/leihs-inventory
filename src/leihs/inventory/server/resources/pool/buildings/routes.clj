(ns leihs.inventory.server.resources.pool.buildings.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.buildings.main :as buildings]
   [leihs.inventory.server.resources.pool.buildings.types :refer [response-body]]
   [leihs.inventory.server.utils.middleware :refer [accept-json-middleware]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/buildings/"

   {:swagger {:tags [""]}}

   [""
    {:get {:summary (fe "")
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-middleware]
           :swagger {:produces ["application/json"]}
           :parameters {:path {:pool_id s/Uuid}}
           :handler buildings/index-resources
           :responses {200 {:description "OK"
                            :body [response-body]}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]])
