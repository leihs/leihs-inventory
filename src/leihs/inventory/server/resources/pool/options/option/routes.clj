(ns leihs.inventory.server.resources.pool.options.option.routes
  (:require
   [leihs.inventory.server.resources.pool.options.option.main :as option]
   [leihs.inventory.server.resources.pool.options.types :refer [response-option-object]]
   [leihs.inventory.server.utils.auth.role-auth :refer [permission-by-role-and-pool]]
   [leihs.inventory.server.utils.auth.roles :as roles]
   [reitit.coercion.spec :as spec]
   [ring.middleware.accept]))

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
          :coercion spec/coercion
          :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
          :parameters {:path {:pool_id uuid?
                              :option_id uuid?}
                       :body :option/body}
          :handler option/put-resource
          :responses {200 {:description "OK"
                           :body response-option-object}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}

    :delete {:accept "application/json"
             :coercion spec/coercion
             :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
             :parameters {:path {:pool_id uuid?
                                 :option_id uuid?}}
             :handler option/delete-resource
             :responses {200 {:description "OK"
                              :body response-option-object}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}])
