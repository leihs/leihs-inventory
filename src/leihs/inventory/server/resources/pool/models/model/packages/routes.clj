(ns leihs.inventory.server.resources.pool.models.model.packages.routes
  (:require
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.models.model.packages.main :as packages]
   [leihs.inventory.server.resources.pool.models.model.packages.types :as ty]
   [leihs.inventory.server.utils.auth.role-auth :refer [permission-by-role-and-pool]]
   [leihs.inventory.server.utils.auth.roles :as roles]
   [leihs.inventory.server.utils.middleware :refer [accept-json-middleware]]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/models/:model_id/packages/"
   {:post {:accept "application/json"
           :coercion spec/coercion
           :parameters {:path {:pool_id uuid?
                               :model_id uuid?}
                        :body :package/payload}
           :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
           :handler packages/post-resource
           :responses {200 {:description "OK"
                            :body :package-put-response2/inventory-item}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}

    :get {:accept "application/json"
          :description "Permitted access for:\n- lending_manager\n- inventory_manager"
          :coercion spec/coercion
          :parameters {:path {:pool_id uuid?
                              :model_id uuid?}}
          :handler packages/index-resources
          :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
          :responses {200 {:body :package-get-response/inventory-item
                           :description "OK"}
                      400 {:description "The inventory code is invalid or outdated"}
                      401 {:description "Unauthorized: invalid role for the requested pool or method"
                           :body {:error string?}}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}}])
