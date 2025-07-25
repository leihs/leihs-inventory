(ns leihs.inventory.server.resources.pool.software.routes
  (:require
   [clojure.spec.alpha :as sa]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.software.main :as software]

   [leihs.inventory.server.resources.pool.software.types :refer [response-option-get
                                                                response-option-post]]
   [leihs.inventory.server.utils.auth.role-auth :refer [permission-by-role-and-pool]]
   [leihs.inventory.server.utils.auth.roles :as roles]
   [leihs.inventory.server.utils.coercion.core :refer [Date]]
   [leihs.inventory.server.utils.middleware :refer [accept-json-middleware]]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/software/"
   {:post {:accept "application/json"
           :summary "(DEV) | Form-Handler: Fetch form data [v0]"
           :swagger {:consumes ["multipart/form-data"]
                     :produces "application/json"}
           :coercion spec/coercion
           :parameters {:path {:pool_id uuid?}
                        :multipart :software/multipart}
           :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
           :handler software/create-software-handler-by-pool-form
           :responses {200 {:description "OK"
                            :body {:data :software/response
                                   :validation [any?]}}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}])
