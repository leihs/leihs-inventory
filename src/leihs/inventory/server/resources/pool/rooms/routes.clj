(ns leihs.inventory.server.resources.pool.rooms.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.rooms.main :as rooms]
   [leihs.inventory.server.resources.pool.rooms.types :refer [get-response]]
   [leihs.inventory.server.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  [""
   {:swagger {:tags [""]}}

   ["/rooms/"

    [""
     {:get {:summary (fe "")
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware]
            :swagger {:produces ["application/json"]}
            :parameters {:query {(s/optional-key :building_id) s/Uuid}}
            :handler rooms/index-resources
            :responses {200 {:description "OK"
                             :body [get-response]}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]]])
