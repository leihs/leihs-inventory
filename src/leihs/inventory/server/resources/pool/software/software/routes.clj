(ns leihs.inventory.server.resources.pool.software.software.routes
  (:require
   [leihs.inventory.server.resources.pool.software.software.main :as software]
   [leihs.inventory.server.resources.pool.software.software.types :as types]
   [leihs.inventory.server.utils.auth.role-auth :refer [permission-by-role-and-pool]]
   [leihs.inventory.server.utils.auth.roles :as roles]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [ring.middleware.accept]))

(defn routes []
  ["/software/:model_id"
   {:get {:accept "application/json"
          :coercion spec/coercion
          :parameters {:path {:pool_id uuid?
                              :model_id uuid?}}
          :handler software/get-resource
          :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
          :responses {200 {:description "OK"
                           :body ::types/put-response}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}

    :put {:accept "application/json"
          :coercion spec/coercion
          :parameters {:path {:pool_id uuid?
                              :model_id uuid?}
                       :body :software-put/multipart}
          :handler software/put-resource
          :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
          :responses {200 {:description "OK"
                           :body ::types/put-response}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}

    :delete {:accept "application/json"
             :coercion spec/coercion
             :parameters {:path {:pool_id uuid?
                                 :model_id uuid?}}
             :handler software/delete-resource
             :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
             :responses {200 {:description "OK"
                              :body types/delete-response}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}])
