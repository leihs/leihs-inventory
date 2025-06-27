(ns leihs.inventory.server.resources.pool.images.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.pool.images.main :refer [get-image-thumbnail-handler]]
   [leihs.inventory.server.resources.pool.models.main :refer [get-models-handler
                                                         create-model-handler
                                                         update-model-handler
                                                         delete-model-handler]]
   [leihs.inventory.server.resources.pool.models.models-by-pool :refer [get-models-of-pool-handler
                                                                   create-model-handler-by-pool
                                                                   get-models-of-pool-handler
                                                                   update-model-handler-by-pool
                                                                   delete-model-handler-by-pool]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware accept-json-image-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn get-images-routes []
  ["/"
   {:swagger {:conflicting true
              :tags ["Images [fe]"]}}

   ["images/:id/thumbnail"
    {:get {:conflicting true
           :description "Determines image thumbnail by targetID"
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-image-middleware]
           :swagger {:produces ["application/json" "image/jpeg"]}
           :parameters {:path {:id s/Uuid}}
           :handler get-image-thumbnail-handler
           :responses {200 {:description "OK"}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]

   ["images/:id"
    {:get {:conflicting true
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-image-middleware]
           :swagger {:produces ["application/json" "image/jpeg"]}
           :parameters {:path {:id s/Uuid}}
           :handler get-image-thumbnail-handler
           :responses {200 {:description "OK"}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]

   ["images/"
    {:get {:conflicting true
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-middleware]
           :swagger {:produces ["application/json"]}
           :handler get-image-thumbnail-handler
           :responses {200 {:description "OK"}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]])
