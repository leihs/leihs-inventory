(ns leihs.inventory.server.resources.items.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.items.main :refer [get-items-of-pool-handler
                                                        get-items-of-pool-with-pagination-handler]]
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

(defn get-items-routes []
  [""

   [""
    {:swagger {:conflicting true
               :tags ["Items"] :security []}}

    ["/items"
     {:get {:conflicting true
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware]
            :swagger {:produces ["application/json"]}

            :parameters {;:path {:pool_id s/Uuid}

                         :query {(s/optional-key :page) s/Int
                                 (s/optional-key :size) s/Int
                              ;(s/optional-key :sort_by) (s/enum :manufacturer-asc :manufacturer-desc :product-asc :product-desc)
                              ;(s/optional-key :filter_manufacturer) s/Str
                              ;(s/optional-key :filter_product) s/Str}
                                 }}

            :handler get-items-of-pool-with-pagination-handler
            :responses {200 {:description "OK"
                             :body s/Any}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]

    ["/items/:item_id"
     {:get {:conflicting true
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware]
            :swagger {:produces ["application/json"]}
            :parameters {:path {;:pool_id s/Uuid
                                :item_id s/Uuid}}
            :handler get-items-of-pool-handler
            :responses {200 {:description "OK"
                             :body s/Any}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]]

   ["/:pool_id"
    {:swagger {:conflicting true
               :tags ["Items by pool"] :security []}}

    ["/items"
     {:get {:description "https://staging.leihs.zhdk.ch/manage/8bd16d45-056d-5590-bc7f-12849f034351/items"
            :conflicting true
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware]
            :swagger {:produces ["application/json"]}
           ;:parameters {:path {:pool_id s/Uuid}}

            :parameters {:path {:pool_id s/Uuid}

                         :query {(s/optional-key :page) s/Int
                                 (s/optional-key :size) s/Int
                                ;(s/optional-key :sort_by) (s/enum :manufacturer-asc :manufacturer-desc :product-asc :product-desc)
                                ;(s/optional-key :filter_manufacturer) s/Str
                                ;(s/optional-key :filter_product) s/Str}
                                 }}

            :handler get-items-of-pool-with-pagination-handler
            :responses {200 {:description "OK"
                             :body s/Any}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]

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