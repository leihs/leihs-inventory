(ns leihs.inventory.server.resources.pool.models-compatibles.routes
  (:require
   [clojure.spec.alpha :as sa]
   [clojure.string :as str]
   [leihs.inventory.server.resources.pool.models-compatibles.main :refer [get-models-compatible-handler]]
   [leihs.inventory.server.resources.pool.models.coercion :as mc]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.auth.roles :as roles]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(def compatible-data {(s/optional-key :cover_image_id) s/Uuid
                      (s/optional-key :cover_image_url) s/Str
                      :model_id s/Any
                      :product s/Str
                      (s/optional-key :version) s/Str})

(def compatible-response
  (s/->Either [[compatible-data] {:data [compatible-data] :pagination s/Any}]))

(defn get-models-compatibles-route []
  ["/:pool_id/"
   {:swagger {:conflicting true
              :tags []}}

   ["models/compatibles/"
    {:get {:conflicting true
           :summary "[fe]"
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-middleware]
           :swagger {:produces ["application/json"]}

           :parameters {:query {(s/optional-key :page) s/Int
                                (s/optional-key :size) s/Int
                                ;(s/optional-key :sort_by) (s/enum :manufacturer-asc :manufacturer-desc :product-asc :product-desc)
                                ;(s/optional-key :filter_manufacturer) s/Str
                                ;(s/optional-key :filter_product) s/Str
                                }}
           :handler get-models-compatible-handler
           :responses {200 {:description "OK"
                            :body compatible-response}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]

   ;["models-compatibles/:model_id"
   ; {:get {:conflicting true
   ;        :summary "[fe]"
   ;        :accept "application/json"
   ;        :coercion reitit.coercion.schema/coercion
   ;        :middleware [accept-json-middleware]
   ;        :swagger {:produces ["application/json"]}
   ;        :parameters {:path {:model_id s/Uuid}}
   ;        :handler get-models-compatible-handler
   ;        :responses {200 {:description "OK"
   ;                         :body s/Any}
   ;                    404 {:description "Not Found"}
   ;                    500 {:description "Internal Server Error"}}}}]

   ])
