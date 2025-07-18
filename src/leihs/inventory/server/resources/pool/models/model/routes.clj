(ns leihs.inventory.server.resources.pool.models.model.routes
  (:require
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.models.model.main :as model]
   [leihs.inventory.server.resources.pool.models.model.types :refer [delete-response
                                                                     patch-response
                                                                     put-response]]
   [leihs.inventory.server.resources.pool.models.types :refer [description-model-form]]
   [leihs.inventory.server.utils.auth.role-auth :refer [permission-by-role-and-pool]]
   [leihs.inventory.server.utils.auth.roles :as roles]
   [leihs.inventory.server.utils.middleware :refer [accept-json-middleware]]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
   ["/models/:model_id"
      {:get {:accept "application/json"
             :summary (fe "Form-Handler: Fetch model")
             :coercion spec/coercion
             :parameters {:path {:pool_id uuid?
                                 :model_id uuid?}}
             :middleware [accept-json-middleware (permission-by-role-and-pool roles/min-role-lending-manager)]
             :handler model/get-resource
             :responses {200 {:description "OK"
                              :body :model-get-put-response/inventory-model}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}

       :patch {:accept "application/json"
               :summary (fe "Form-Handler: Used to patch model-attributes")
               :coercion reitit.coercion.schema/coercion
               :description description-model-form
               :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
               :parameters {:path {:pool_id s/Uuid
                                   :model_id s/Uuid}
                            :body {:is_cover (s/maybe s/Uuid)}}
               :handler model/patch-resource
               :responses {200 {:description "OK"
                                :body patch-response}
                           404 {:description "Not Found"}
                           500 {:description "Internal Server Error"}}}

       :delete {:accept "application/json"
                :summary (fe "Form-Handler: Delete model")
                :swagger {:consumes ["multipart/form-data"]
                          :produces "application/json"}
                :description description-model-form
                :coercion spec/coercion
                :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
                :parameters {:path {:pool_id uuid?
                                    :model_id uuid?}}
                :handler model/delete-resource
                :responses {200 {:description "OK"
                                 :body delete-response}
                            404 {:description "Not Found"}
                            500 {:description "Internal Server Error"}}}

       :put {:accept "application/json"
             :summary (fe "Form-Handler: Update model")
             :coercion spec/coercion
             :description description-model-form
             :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
             :parameters {:path {:pool_id uuid?
                                 :model_id uuid?}
                          :body :model/multipart}
             :handler model/put-resource
             :responses {200 {:description "OK"
                              :body put-response}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}])
