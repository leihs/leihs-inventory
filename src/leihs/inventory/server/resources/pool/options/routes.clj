(ns leihs.inventory.server.resources.pool.options.routes
  (:require
   [clojure.spec.alpha :as sa]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.options.main :as options]
   [leihs.inventory.server.resources.pool.options.types :as ty :refer [response-option-post]]
   [leihs.inventory.server.utils.auth.role-auth :refer [permission-by-role-and-pool]]
   [leihs.inventory.server.utils.auth.roles :as roles]
   [leihs.inventory.server.utils.coercion.core :refer [Date]]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/options/"
   {:post {:accept "application/json"
           :coercion spec/coercion
           :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
           :parameters {:path {:pool_id uuid?}
                        :body :option/multipart}
           :handler options/post-resource
           :responses {200 {:description "OK"
                            :body response-option-post}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}

    :get {:accept "application/json"
          :coercion spec/coercion
          :parameters {:path {:pool_id uuid?}
                       :query ::ty/options-query}
          :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
          :handler options/index-resources
          :responses {200 {:description "OK"
                           ;:body :ty/response-option-get}
                           ;:body ::ty/response-options-get
                           }
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}}])
