(ns leihs.inventory.server.resources.pool.models.model.images.image.thumbnail.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.pool.models.model.images.image.thumbnail.main :refer [get-image-thumbnail-handler]]

   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware accept-json-image-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn get-models-images-single-thumbnail-routes []
  ["/:pool_id/"
   {:swagger {:tags [""]}}

   ["models/:model_id/images/:image_id/thumbnail"
    {:get {:description "Determines image thumbnail by targetID"
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-image-middleware]
           :swagger {:produces ["application/json" "image/jpeg"]}
           :parameters {:path {
                               :pool_id s/Uuid
                               :model_id s/Uuid
                               :image_id s/Uuid
                               }}
           :handler get-image-thumbnail-handler
           :responses {200 {:description "OK"}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]])
