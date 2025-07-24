(ns leihs.inventory.server.resources.pool.options.option.routes
  (:require
   [leihs.inventory.server.constants :refer [fe]]

   [leihs.inventory.server.resources.pool.options.coercion ]

   [leihs.inventory.server.resources.pool.options.option.main :as option]
   ;[leihs.inventory.server.resources.pool.options.option.types :refer [description-model-form
   ;                                                            post-response
   ;                                                            get-compatible-response]]

   [leihs.inventory.server.resources.pool.options.types :refer [response-option-get
                                                                response-option-post
                                                                response-option-object
                                                                ]]

   [leihs.inventory.server.utils.auth.role-auth :refer [permission-by-role-and-pool]]
   [leihs.inventory.server.utils.auth.roles :as roles]
   [leihs.inventory.server.utils.coercion.core :refer [Date]]
   [leihs.inventory.server.utils.middleware :refer [accept-json-middleware]]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  [
  "/options/:option_id"
   {:get {:accept "application/json"
          :summary "(DEV) | Form-Handler: Fetch form data [v0]"
          :coercion spec/coercion
          :parameters {:path {:pool_id uuid?
                              :option_id uuid?}}
          :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
          :handler option/fetch-option-handler-by-pool-form
          :responses {200 {:description "OK"
                           ;:body mc/response-option
                           :body response-option-object
                           }
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}

    :put {:accept "application/json"
          :swagger {:consumes ["multipart/form-data"]
                    :produces "application/json"}
          :summary "(DEV) | [v0]"
          :coercion spec/coercion
          :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
          :parameters {:path {:pool_id uuid?
                              :option_id uuid?}
                       :multipart :option/multipart}
          :handler option/update-option-handler-by-pool-form
          :responses {200 {:description "OK"
                           ;:content_type "multipart/form-data"
                           ;:body mc/response-option
                           :body response-option-object
                           }
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}}
]
)
