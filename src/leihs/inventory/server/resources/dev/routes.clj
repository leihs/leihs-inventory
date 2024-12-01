(ns leihs.inventory.server.resources.dev.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.categories.main :refer [get-model-groups-of-pool-handler
                                                             get-groups-of-pool-handler
                                                             get-entitlement-groups-of-pool-handler
                                                             get-model-group-links-of-pool-handler]]
   [leihs.inventory.server.resources.models.main :refer [get-models-handler
                                                         create-model-handler
                                                         update-model-handler
                                                         delete-model-handler]]

   [leihs.inventory.server.resources.dev.main :refer [update-and-fetch-accounts]]

   [leihs.inventory.server.resources.models.models-by-pool :refer [get-models-of-pool-handler
                                                                   create-model-handler-by-pool
                                                                   get-models-of-pool-handler
                                                                   update-model-handler-by-pool
                                                                   delete-model-handler-by-pool]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn create-description [url]
  (str "- GET " url " Accept: application/json "))

(defn get-dev-routes []

  [""

   ["/dev"
    {:swagger {:conflicting true
               :tags ["Dev"] :security []}}
    ["/update-accounts" {:get {:conflicting true
               :summary "Overwrite pw for accounts with various roles OR is_admin"
               :description "Fetch accounts with variants of:\n
               - role: inventory_manager, lending_manager, group_manager, customer\n
               - is_admin: true\n"
               :accept "application/json"
               :coercion reitit.coercion.schema/coercion
               ;:middleware [accept-json-middleware]
               :swagger {:produces ["application/json"]}
              ;:parameters {:path {:pool_id s/Uuid}}
               :handler update-and-fetch-accounts
               :responses {200 {:description "OK"
                                :body s/Any}
                           404 {:description "Not Found"}
                           500 {:description "Internal Server Error"}}}}]


    ]



   ])
