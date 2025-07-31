(ns leihs.inventory.server.resources.pool.owners.owner.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.owners.owner.main :as owner]
   [leihs.inventory.server.resources.pool.owners.types :refer [response-body]]
   [leihs.inventory.server.utils.middleware :refer [accept-json-middleware]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(def resp-owners {:id s/Uuid
                  :name s/Str})

(defn routes []
  ["/owners/:id"
   {:get {:accept "application/json"
          :coercion reitit.coercion.schema/coercion
          :parameters {:path {:pool_id s/Uuid
                              :id s/Uuid}}
          :handler owner/get-resource
          :responses {200 {:description "OK"
                           :body resp-owners}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}}])
