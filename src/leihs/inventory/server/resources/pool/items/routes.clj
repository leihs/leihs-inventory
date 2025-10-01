(ns leihs.inventory.server.resources.pool.items.routes
  (:require
   [leihs.inventory.server.resources.pool.items.main :as items]
   [leihs.inventory.server.resources.pool.items.types :as types]
   [leihs.inventory.server.utils.middleware :refer [accept-json-middleware]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/items/"
   {:get {:accept "application/json"
          :coercion reitit.coercion.schema/coercion
          :middleware [accept-json-middleware]
          :swagger {:produces ["application/json"]}
          :summary "Returns all items/packages of a pool filtered by query parameters"
          :parameters {:path types/path-params
                       :query types/query-params}
          :handler items/index-resources
          :responses {200 {:description "OK"
                           :body types/get-items-response}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}}])
