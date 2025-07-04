(ns leihs.inventory.server.resources.pool.accessories.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.pool.accessories.main :refer [get-accessories-of-pool-handler get-accessories-handler]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn get-accessories-routes []

  ["/"
   {:swagger {:tags [""]}}

   [":pool_id/models/:model_id"
    {:swagger {:tags [""]}}

    ["/accessories/:id"
     {:get {:accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware]
            :swagger {:produces ["application/json"]}
            :parameters {:path {:pool_id s/Uuid :id s/Uuid}}
            :handler get-accessories-of-pool-handler
            :responses {200 {:description "OK"
                             :body s/Any}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]

    ["/accessories/"
     {:get {:accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware]
            :swagger {:produces ["application/json"]}
            :parameters {:path {:pool_id s/Uuid}}
            :handler get-accessories-of-pool-handler
            :responses {200 {:description "OK"
                             :body s/Any}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]]])
