(ns leihs.inventory.server.resources.pool.models.model.images.image.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.models.model.images.image.constants :refer [ALLOWED_IMAGE_CONTENT_TYPES]]
   [leihs.inventory.server.resources.pool.models.model.images.image.main :as image]
   [leihs.inventory.server.resources.pool.models.model.images.image.types :refer [delete-response
                                                                                  error-image-not-found
                                                                                  image]]
   [leihs.inventory.server.resources.pool.models.model.images.types :as images]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-image-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [next.jdbc :as jdbc]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/:pool_id/"
   {:swagger {:tags [""]}}

   ["models/:model_id/images/:image_id"
    {:get {:summary (fe "")
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-image-middleware]
           :swagger {:produces (into ["application/json"] ALLOWED_IMAGE_CONTENT_TYPES)}
           :parameters {:path {:pool_id s/Uuid
                               :model_id s/Uuid
                               :image_id s/Uuid}}
           :handler image/get-resource
           :responses {200 {:description "OK"
                            :body (s/->Either [image s/Any])}
                       404 {:description "Not Found"
                            :body error-image-not-found}
                       500 {:description "Internal Server Error"}}}

     :delete {:accept "application/json"
              :summary (fe "")
              :coercion reitit.coercion.schema/coercion
              :parameters {:path {:pool_id s/Uuid
                                  :model_id s/Uuid
                                  :image_id s/Uuid}}
              :handler image/delete-resource
              :responses {200 {:description "OK"
                               :body delete-response}
                          404 {:description "Not Found"
                               :body error-image-not-found}
                          500 {:description "Internal Server Error"}}}}]])
