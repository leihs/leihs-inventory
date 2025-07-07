(ns leihs.inventory.server.resources.pool.responsible-inventory-pools.routes
  (:require
   [clojure.set]
   [leihs.core.auth.session :refer [wrap-authenticate]]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.responsible-inventory-pools.main :refer [get-responsible-pools-handler]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware wrap-is-admin!]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn get-responsible-inventory-pools-routes []
  [""
   {:swagger {:tags [""]}}

   ["/:pool_id/responsible-inventory-pools/"
    {:get {:summary (fe "")
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :middleware [wrap-authenticate accept-json-middleware]
           :swagger {:produces ["application/json"]}
           :parameters {:path {:pool_id s/Uuid}}
           :handler get-responsible-pools-handler
           :responses {200 {:description "OK"
                            :body s/Any}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]])
