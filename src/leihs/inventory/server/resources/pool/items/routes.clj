(ns leihs.inventory.server.resources.pool.items.routes
  (:require
   [leihs.inventory.server.resources.pool.items.main :as items]
   [leihs.inventory.server.resources.pool.items.types :as types :refer [query-params]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/items/"
   {:get {:accept "application/json"
          :coercion reitit.coercion.schema/coercion
          :swagger {:produces ["application/json"]}
          :summary "Returns all items/packages of a pool filtered by query parameters"
          :parameters {:path types/path-params
                       :query types/query-params}
          :handler items/index-resources
          :produces ["application/json"]
          :responses {200 {:description "OK"
                           :body s/Any}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}}])
