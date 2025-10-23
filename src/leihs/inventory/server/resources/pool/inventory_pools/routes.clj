(ns leihs.inventory.server.resources.pool.inventory-pools.routes
  (:require
   [clojure.set]
   [leihs.core.auth.session :refer [wrap-authenticate]]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.inventory-pools.main :as inventory-pools]
   [leihs.inventory.server.resources.pool.inventory-pools.types :refer [get-response]]
   [leihs.inventory.server.utils.middleware :refer [accept-json-middleware]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/inventory-pools/"
   {:get {:summary (fe "")
          :accept "application/json"
          :coercion reitit.coercion.schema/coercion
          :middleware [wrap-authenticate accept-json-middleware]
          :swagger {:produces ["application/json"]}
          :parameters {:path {:pool_id s/Uuid}
                       :query {(s/optional-key :responsible) s/Bool}}
          :handler inventory-pools/get-resources
          :responses {200 {:description "OK"
                           :body get-response}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}}])
