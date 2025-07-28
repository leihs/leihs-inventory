(ns leihs.inventory.server.resources.pool.options.option.routes
  (:require
   [leihs.inventory.server.resources.pool.options.option.main :as option]
   [leihs.inventory.server.resources.pool.options.types :refer [
                                                                ;response-option-get
                                                                response-option-post
                                                                response-option-object]]
   [leihs.inventory.server.utils.auth.role-auth :refer [permission-by-role-and-pool]]
   [leihs.inventory.server.utils.auth.roles :as roles]
   [leihs.inventory.server.utils.coercion.core :refer [Date]]
   [leihs.inventory.server.utils.middleware :refer [accept-json-middleware]]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/options/:option_id"
   {:get {:accept "application/json"
          :coercion spec/coercion
          :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
          :parameters {:path {:pool_id uuid?
                              :option_id uuid?}}
          :handler option/get-resource
          :responses {200 {:description "OK"
                           :body response-option-object}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}

    :put {:accept "application/json"
          ;:swagger {:consumes ["multipart/form-data"]
          ;          :produces "application/json"}
          :coercion spec/coercion
          :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
          :parameters {:path {:pool_id uuid?
                              :option_id uuid?}
                       :body :option/multipart}
          :handler option/put-resource
          :responses {200 {:description "OK"
                           :body response-option-object}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}}])
