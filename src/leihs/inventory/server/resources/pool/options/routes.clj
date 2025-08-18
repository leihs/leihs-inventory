(ns leihs.inventory.server.resources.pool.options.routes
  (:require
   [leihs.inventory.server.resources.pool.options.main :as options]
   [leihs.inventory.server.resources.pool.options.types :as types]
   [leihs.inventory.server.utils.auth.role-auth :refer [permission-by-role-and-pool]]
   [leihs.inventory.server.utils.auth.roles :as roles]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [ring.middleware.accept]))

(defn routes []
  ["/options/"
   {:post {:accept "application/json"
           :coercion spec/coercion
           :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
           :parameters {:path {:pool_id uuid?}
                        :body ::types/post-option-body}
           :handler options/post-resource
           :responses {200 {:description "OK"
                            :body types/response-option-post}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}

    :get {:accept "application/json"
          :coercion spec/coercion
          :parameters {:path {:pool_id uuid?}
                       :query ::types/get-options-query}
          :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
          :handler options/index-resources
          :responses {200 {:description "OK"
                           :body types/response-option-get}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}}])
