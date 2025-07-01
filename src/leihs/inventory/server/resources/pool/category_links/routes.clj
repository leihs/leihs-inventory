(ns leihs.inventory.server.resources.pool.category-links.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.pool.category-links.main :refer [;get-model-groups-of-pool-handler
                                                             ;get-groups-of-pool-handler
                                                             ;get-entitlement-groups-of-pool-handler
                                                                      get-model-group-links-of-pool-handler]]
   ;[leihs.inventory.server.resources.models.main :refer [get-models-handler
   ;                                                      create-model-handler
   ;                                                      update-model-handler
   ;                                                      delete-model-handler]]
   ;[leihs.inventory.server.resources.models.models-by-pool :refer [get-models-of-pool-handler
   ;                                                                create-model-handler-by-pool
   ;                                                                get-models-of-pool-handler
   ;                                                                update-model-handler-by-pool
   ;                                                                delete-model-handler-by-pool]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn create-description [url]
  (str "- GET " url " Accept: application/json "))

(defn get-category-links-routes []

  ["/:pool_id"

   ["/category_links"
    {:swagger {:conflicting true
               :tags []}}
    ["/" {:get {:conflicting true
                :summary "Kategorie-Links anzeigen / category_links == model_group_links"
                :description (create-description "https://staging.leihs.zhdk.ch/category_links")
                :accept "application/json"
                :coercion reitit.coercion.schema/coercion
                :middleware [accept-json-middleware]
                :swagger {:produces ["application/json"]}
              ;:parameters {:path {:pool_id s/Uuid}}
                :handler get-model-group-links-of-pool-handler
                :responses {200 {:description "OK"
                                 :body [{:id s/Uuid
                                         :parent_id s/Uuid
                                         :child_id s/Uuid
                                         :label s/Str}]}
                            404 {:description "Not Found"}
                            500 {:description "Internal Server Error"}}}}]]])
