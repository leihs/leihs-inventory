(ns leihs.inventory.server.resources.user.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.models.main :refer [get-models-handler
                                                         create-model-handler
                                                         update-model-handler
                                                         delete-model-handler]]
   [leihs.inventory.server.resources.models.models-by-pool :refer [get-models-of-pool-handler
                                                                   create-model-handler-by-pool
                                                                   get-models-of-pool-handler
                                                                   update-model-handler-by-pool
                                                                   delete-model-handler-by-pool]]
   [leihs.inventory.server.resources.user.main :refer [get-pools-of-user-handler get-user-details-handler]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(def schema-min
  {:id s/Uuid
   :name s/Str
   (s/optional-key :description) (s/maybe s/Str)})

(defn get-user-routes []
  ["/"
   {:swagger {:conflicting true
              :tags ["User"] :security []}}

   ["pools/:user_id"
    {:get {:conflicting true
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-middleware]
           :swagger {:produces ["application/json"]}
           :parameters {:path {:user_id s/Uuid}}
           :handler get-pools-of-user-handler
           :responses {200 {:description "OK"
                            :body [schema-min]}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]

   ["details/:user_id"
    {:get {:conflicting true
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-middleware]
           :swagger {:produces ["application/json"]}
           :parameters {:path {:user_id s/Uuid}}
           :handler get-user-details-handler
           :responses {200 {:description "OK"
                            :body s/Any}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]])
