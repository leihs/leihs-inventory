(ns leihs.inventory.server.resources.pool.items.item.routes
  (:require
   [cheshire.core :as json]
   [clojure.set]
   [clojure.set :as set]
   [leihs.inventory.server.resources.pool.items.item.main :refer [get-items-of-pool-handler]]

   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.resources.utils.request :refer [query-params]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [ring.util.response :as response]
   [schema.core :as s]))

(defn get-items-item-routes []
  [""

   ["/:pool_id"
    {:swagger {:conflicting true
               :tags []}}

    ["/items/:item_id"
     {:get {:conflicting true
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware]
            :swagger {:produces ["application/json"]}
            :parameters {:path {:pool_id s/Uuid :item_id s/Uuid}}
            :handler get-items-of-pool-handler
            :responses {200 {:description "OK"
                             :body s/Any}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]]])
