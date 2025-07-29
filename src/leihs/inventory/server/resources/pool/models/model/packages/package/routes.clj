(ns leihs.inventory.server.resources.pool.models.model.packages.package.routes
  (:require
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.models.model.packages.package.main :as package]
   [leihs.inventory.server.resources.pool.models.model.packages.package.types :as ty]
   [leihs.inventory.server.utils.auth.role-auth :refer [permission-by-role-and-pool]]
   [leihs.inventory.server.utils.auth.roles :as roles]
   [leihs.inventory.server.utils.middleware :refer [accept-json-middleware]]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/models/:model_id/packages/:item_id"
   {:put {:accept "application/json"
          :coercion spec/coercion
          :parameters {:path {:pool_id uuid?
                              :model_id uuid?
                              :item_id uuid?}
                       :body :package/payload}
          :handler package/put-resource
          :responses {200 {:description "OK"
                           :body :package-put-response2/inventory-item}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}

    :get {:accept "application/json"
          :coercion spec/coercion
          :parameters {:path {:pool_id uuid?
                              :model_id uuid?
                              :item_id uuid?}}
          :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
          :handler package/get-resource
          :responses {200 {:description "OK"
                           :body :package-put-response2/inventory-item}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}}])
