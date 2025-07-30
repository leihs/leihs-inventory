(ns leihs.inventory.server.resources.pool.packages.routes
  (:require
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.packages.main :as packages]
   [leihs.inventory.server.resources.pool.packages.types :as ty]
   ;[leihs.inventory.server.resources.pool.packages.types :refer [description-model-form
   ;                                                            get-compatible-response
   ;                                                            post-response]]
   [leihs.inventory.server.utils.auth.role-auth :refer [permission-by-role-and-pool]]
   [leihs.inventory.server.utils.auth.roles :as roles]
   [leihs.inventory.server.utils.middleware :refer [accept-json-middleware]]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/packages/"
   {:post {:accept "application/json"
           ;:swagger {:consumes ["multipart/form-data"]
           ;          :produces "application/json"}
           :summary "(DEV) | Dynamic-Form-Handler: Fetch form data | Fetch fields by Role [v0]"
           :coercion spec/coercion
           :parameters {:path {:pool_id uuid?}
                        :body :package/payload}
           :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
           :handler packages/create-package-handler-by-pool-form
           :responses {200 {:description "OK"
                            ;:body :package-put-response2/inventory-item
                            }
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}

    :get {:accept "application/json"
          :summary "(DEV) | Dynamic-Form-Handler: Fetch form data | Fetch fields by Role [v0]"
          :description "Permitted access for:\n- lending_manager\n- inventory_manager"
          :coercion spec/coercion
          :parameters {:path {:pool_id uuid?}}
          :handler packages/fetch-package-handler-by-pool-form
          :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
          :responses {200 {
                           ;:body {:data {:inventory_code string?
                           ;              :inventory_pool_id uuid?
                           ;              :responsible_department any?}
                           ;       :fields [any?]}
                           :description "OK"}
                      401 {:description "Unauthorized: invalid role for the requested pool or method"
                           :body {:error string?}}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}}])
