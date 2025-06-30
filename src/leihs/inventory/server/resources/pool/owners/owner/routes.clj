(ns leihs.inventory.server.resources.pool.owners.owner.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.pool.owners.owner.main :refer [get-owner-department-of-pool-auto-pagination-handler]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.coercion.core :refer [pagination]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(def resp-owners [{:id s/Any
                   :name s/Str}])

(defn get-owners-single-routes []
  ["/:pool_id"
   {:swagger {:conflicting true
              :tags []}}

   ["/owners"

    ["/:id"
     {:get {:conflicting true
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware]
            :swagger {:produces ["application/json"]}
            :parameters {:path {:id s/Uuid}}
            :handler get-owner-department-of-pool-auto-pagination-handler
            :responses {200 {:description "OK"
                             :body resp-owners}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]]
   ])
