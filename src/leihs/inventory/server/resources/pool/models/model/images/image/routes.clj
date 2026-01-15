(ns leihs.inventory.server.resources.pool.models.model.images.image.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.pool.models.model.images.image.constants :refer [ALLOWED_IMAGE_CONTENT_TYPES]]
   [leihs.inventory.server.resources.pool.models.model.images.image.main :as image]
   [leihs.inventory.server.resources.pool.models.model.images.image.types :refer [delete-response
                                                                                  error-message-structure
                                                                                  image]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/models/:model_id/images/:image_id"
   {:get {:coercion reitit.coercion.schema/coercion
          :swagger {:produces (into ["application/json"] ALLOWED_IMAGE_CONTENT_TYPES)}
          :produces (into ["application/json"] ALLOWED_IMAGE_CONTENT_TYPES)
          :parameters {:path {:pool_id s/Uuid
                              :model_id s/Uuid
                              :image_id s/Uuid}}
          :handler image/get-resource
          :responses {200 {:description "OK"}
                      404 {:description "Not Found"}
                      406 {:description "Requested content type not supported"}
                      500 {:description "Internal Server Error"}}}

    :delete {:accept "application/json"
             :coercion reitit.coercion.schema/coercion
             :parameters {:path {:pool_id s/Uuid
                                 :model_id s/Uuid
                                 :image_id s/Uuid}}
             :produces ["application/json"]
             :handler image/delete-resource
             :responses {200 {:description "OK"
                              :body delete-response}
                         404 {:description "Not Found"
                              :body error-message-structure}
                         500 {:description "Internal Server Error"}}}}])
