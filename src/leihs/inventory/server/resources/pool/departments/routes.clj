(ns leihs.inventory.server.resources.pool.departments.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.pool.departments.main :refer [get-owner-department-of-pool-auto-pagination-handler]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.coercion.core :refer [pagination]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn get-departments-routes []
  ["/:pool_id"
   {:swagger {:conflicting true
              :tags []}}

   ["/departments/"
    ["" {:get {:conflicting true
               :description "Form: https://staging.leihs.zhdk.ch/manage/8bd16d45-056d-5590-bc7f-12849f034351/fields?target_type=itemRequest"
               :accept "application/json"
               :coercion reitit.coercion.schema/coercion
               :middleware [accept-json-middleware]
               :swagger {:produces ["application/json"]}
               :parameters {:query {(s/optional-key :page) s/Int
                                    (s/optional-key :size) s/Int}}

               :handler get-owner-department-of-pool-auto-pagination-handler
               :responses {200 {:description "OK"
                                :body s/Any}
                           404 {:description "Not Found"}
                           500 {:description "Internal Server Error"}}}}]]])
