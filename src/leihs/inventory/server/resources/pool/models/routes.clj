(ns leihs.inventory.server.resources.pool.models.routes
  (:require
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.models.main :as models]
   [leihs.inventory.server.resources.pool.models.types :refer [description-model-form
                                                               post-response
                                                               get-compatible-response]]
   [leihs.inventory.server.utils.auth.role-auth :refer [permission-by-role-and-pool]]
   [leihs.inventory.server.utils.auth.roles :as roles]
   [leihs.inventory.server.utils.coercion.core :refer [Date]]
   [leihs.inventory.server.utils.middleware :refer [accept-json-middleware]]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/models/"
   {:get {:conflicting true
          :accept "application/json"
          :coercion reitit.coercion.schema/coercion
          :middleware [accept-json-middleware]
          :swagger {:produces ["application/json"]}
          :summary "Global search for models (-compatibles)"
          :description "Global search for models-compatibles, includes models of type: 'Model', 'Software' as well"
          :parameters {:path {:pool_id s/Uuid}
                       :query {(s/optional-key :page) s/Int
                               (s/optional-key :size) s/Int
                               (s/optional-key :search) s/Str}}
          :handler models/get-resource
          :responses {200 {:description "OK"
                           :body get-compatible-response}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}

    :post {:accept "application/json"
           :summary (fe "Form-Handler: Create model")
           :description description-model-form
           :coercion spec/coercion
           :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
           :parameters {:path {:pool_id uuid?}
                        :body :model/multipart}
           :handler models/post-resource
           :responses {200 {:description "OK"
                            :body post-response}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}])
