(ns leihs.inventory.server.resources.owner-department.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.owner-department.main :refer [get-owner-department-of-pool-auto-pagination-handler]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.coercion.core :refer [pagination]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(def resp-owners [{:id s/Any
                   :name s/Str}])

(defn get-owner-department-routes []
  [""
   {:swagger {:conflicting true
              :tags ["Owner / Department"]}}

   ["/owners"
    ["" {:get {:conflicting true
               :accept "application/json"
               :summary "Get owner department by id [v0]"
               :description "Form: https://staging.leihs.zhdk.ch/manage/8bd16d45-056d-5590-bc7f-12849f034351/fields?target_type=itemRequest"
               :coercion reitit.coercion.schema/coercion
               :middleware [accept-json-middleware]
               :swagger {:produces ["application/json"]}
               :parameters {:query {(s/optional-key :page) s/Int
                                    (s/optional-key :size) s/Int}}
               :handler get-owner-department-of-pool-auto-pagination-handler
               :responses {200 {:description "OK"
                                :body (s/->Either [resp-owners {:data resp-owners
                                                                :pagination pagination}])}
                           404 {:description "Not Found"}
                           500 {:description "Internal Server Error"}}}}]

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

   ["/departments"
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
                           500 {:description "Internal Server Error"}}}}]

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
