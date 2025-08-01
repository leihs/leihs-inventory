(ns leihs.inventory.server.resources.pool.responsible-inventory-pools.routes
  (:require
   [clojure.set]
   [leihs.core.auth.session :refer [wrap-authenticate]]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.responsible-inventory-pools.main :as responsible-inventory-pools]
   [leihs.inventory.server.resources.pool.responsible-inventory-pools.types :refer [get-response]]
   [leihs.inventory.server.utils.middleware :refer [accept-json-middleware]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/responsible-inventory-pools/"
   {:get {:summary (fe "")
          :accept "application/json"
          :coercion reitit.coercion.schema/coercion
          :middleware [wrap-authenticate accept-json-middleware]
          :swagger {:produces ["application/json"]}
          :parameters {:path {:pool_id s/Uuid}}
          :handler responsible-inventory-pools/get-resources
          :responses {200 {:description "OK"
                           :body get-response}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}}])
