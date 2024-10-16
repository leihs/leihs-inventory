(ns leihs.inventory.server.resources.owner-department.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.owner-department.main :refer [
                                                           get-owner-department-of-pool-auto-pagination-handler
                                                           ]]

   ;[leihs.inventory.server.resources.models.main :refer [get-models-handler
   ;                                                      create-model-handler
   ;                                                      update-model-handler
   ;                                                      delete-model-handler]]
   ;[leihs.inventory.server.resources.models.models-by-pool :refer [get-models-of-pool-handler
   ;                                                                create-model-handler-by-pool
   ;                                                                get-models-of-pool-handler
   ;                                                                update-model-handler-by-pool
   ;                                                                delete-model-handler-by-pool]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn create-description [url]
  (str "GET " url " Accept: application/json"))

(defn get-owner-department-routes []
  [""
    {:swagger {:conflicting true
               :tags ["Owner / Department"] :security []}}

   ["/owners"
    ["" {:get {:conflicting true
               :accept "application/json"
               :description "Form: https://https://staging.leihs.zhdk.ch/manage/8bd16d45-056d-5590-bc7f-12849f034351/fields?target_type=itemRequest"
               :coercion reitit.coercion.schema/coercion
               :middleware [accept-json-middleware]
               :swagger {:produces ["application/json"]}

               :parameters {
                            ;:path {:model_id s/Uuid}

                            :query {(s/optional-key :page) s/Int
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
            :parameters {:path {;:pool_id s/Uuid
                                :id s/Uuid}}
            :handler get-owner-department-of-pool-auto-pagination-handler
            :responses {200 {:description "OK"
                             :body s/Any}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]]


   ["/departments"
    ;{:swagger {:conflicting true
    ;           :tags ["Owners / Departments"] :security []}}
    ["" {:get {:conflicting true
               :description "Form: https://https://staging.leihs.zhdk.ch/manage/8bd16d45-056d-5590-bc7f-12849f034351/fields?target_type=itemRequest"
               :accept "application/json"
               :coercion reitit.coercion.schema/coercion
               :middleware [accept-json-middleware]
               :swagger {:produces ["application/json"]}

               :parameters {
                            ;:path {:model_id s/Uuid}

                            :query {(s/optional-key :page) s/Int
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
            :parameters {:path {;:pool_id s/Uuid
                                :id s/Uuid}}
            :handler get-owner-department-of-pool-auto-pagination-handler
            :responses {200 {:description "OK"
                             :body s/Any}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]]

   ])
