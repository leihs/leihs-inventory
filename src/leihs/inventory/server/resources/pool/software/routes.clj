(ns leihs.inventory.server.resources.pool.software.routes
  (:require
   [clojure.spec.alpha :as sa]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.models.basic_coercion :as sp]
   [leihs.inventory.server.resources.pool.software.main :as software]
   [leihs.inventory.server.resources.pool.software.types :as ty]
   [leihs.inventory.server.utils.auth.role-auth :refer [permission-by-role-and-pool]]
   [leihs.inventory.server.utils.auth.roles :as roles]
   [reitit.coercion.spec :as spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/software/"
   {:post {:accept "application/json"
           :summary "(DEV) | Form-Handler: Fetch form data [v0]"
           :coercion spec/coercion
           :parameters {:path {:pool_id uuid?}
                        :body :software-post/multipart}
           :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
           :handler software/index-resources
           :responses {200 {:description "OK"
                            :body ::ty/post-response}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}])
