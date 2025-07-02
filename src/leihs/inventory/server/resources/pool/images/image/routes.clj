(ns leihs.inventory.server.resources.pool.images.image.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.pool.images.image.main :refer [get-image-thumbnail-handler]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware accept-json-image-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn get-images-image-routes []
  ["/:pool_id/"
   {:swagger {:conflicting true
              :tags []}}

   ["images/:id"
    {:get {:conflicting true
           :summary "Get image [fe]"
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-image-middleware]
           :swagger {:produces ["application/json" "image/jpeg"]}
           :parameters {:path {:id s/Uuid}}
           :handler get-image-thumbnail-handler
           :responses {200 {:description "OK"}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]])
