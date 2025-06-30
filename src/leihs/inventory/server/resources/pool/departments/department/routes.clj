(ns leihs.inventory.server.resources.pool.departments.department.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.pool.departments.department.main :refer [get-owner-department-of-pool-auto-pagination-handler]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.coercion.core :refer [pagination]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn get-departments-single-routes []
  [""
   {:swagger {:conflicting true
              :tags ["Owner / Department"]}}

   ["/departments"

    ["/:id"
     {:get {:conflicting true
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware]
            :swagger {:produces ["application/json"]}
            :parameters {:path {:id s/Uuid}}
            :handler get-owner-department-of-pool-auto-pagination-handler
            :responses {200 {:description "OK"
                             :body s/Any}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]]])
