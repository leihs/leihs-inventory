(ns leihs.inventory.server.resources.supplier.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.auth.session :as ab]

   [leihs.inventory.server.resources.models.main :refer [get-models-handler
                                                         create-model-handler
                                                         update-model-handler
                                                         delete-model-handler]]

   [leihs.inventory.server.resources.models.models-by-pool :refer [get-models-of-pool-handler
                                                                   create-model-handler-by-pool
                                                                   get-models-of-pool-handler
                                                                   update-model-handler-by-pool
                                                                   delete-model-handler-by-pool]]
   [leihs.inventory.server.resources.supplier.main :refer [get-suppliers-handler
                                                           get-suppliers-auto-pagination-handler]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn create-description [url]
  (str "GET " url " Accept: application/json"))

(defn get-supplier-routes []

  [""

   ["/supplier"
    {:swagger {:conflicting true
               :tags ["Supplier"] :security []}}
    ["" {:get {:conflicting true
               :description (str "OK-Legacy |"
                                 "Form: https://staging.leihs.zhdk.ch/manage/8bd16d45-056d-5590-bc7f-12849f034351/fields?target_type=itemRequest")
               :accept "application/json"
               :coercion reitit.coercion.schema/coercion
               :middleware [accept-json-middleware ab/wrap]
               :swagger {:produces ["application/json"]}

               :parameters {:query {(s/optional-key :page) s/Int
                                    (s/optional-key :size) s/Int}}

               :handler get-suppliers-auto-pagination-handler
               :responses {200 {:description "OK"
                                :body s/Any}
                           404 {:description "Not Found"}
                           500 {:description "Internal Server Error"}}}}]

    ["/:supplier_id"
     {:get {:conflicting true
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware ab/wrap]

            :swagger {:produces ["application/json"]}
            :parameters {:path {:supplier_id s/Uuid}}
            :handler get-suppliers-auto-pagination-handler
            :responses {200 {:description "OK"
                             :body s/Any}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]]])