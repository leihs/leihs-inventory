(ns leihs.inventory.server.resources.pool.models.model.images.image.thumbnail.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.pool.models.model.images.image.constants :refer [ALLOWED_IMAGE_CONTENT_TYPES]]
   [leihs.inventory.server.resources.pool.models.model.images.image.thumbnail.main :as image-thumbnail]
   [leihs.inventory.server.resources.pool.models.model.images.image.types :as image]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/"
   {:swagger {:tags [""]}}

   ["models/:model_id/images/:image_id/thumbnail"
    {:get {:description "Determines image thumbnail by targetID"
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :swagger {:produces (into ["application/json"] ALLOWED_IMAGE_CONTENT_TYPES)}
           :parameters {:path {:pool_id s/Uuid
                               :model_id s/Uuid
                               :image_id s/Uuid}}
           :handler image-thumbnail/get-resource
           :responses {200 {:description "OK"
                            :body (s/->Either [image/image s/Any])}
                       404 {:description "Not Found"
                            :body image/error-image-not-found}
                       500 {:description "Internal Server Error"}}}}]])
