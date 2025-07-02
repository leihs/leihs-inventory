(ns leihs.inventory.server.resources.pool.models.model.images.image.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.pool.models.model.images.image.main :refer [get-image-thumbnail-handler delete-image]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware accept-json-image-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [next.jdbc :as jdbc]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn get-models-images-image-routes []
  ["/:pool_id/"
   {:swagger {:conflicting true
              :tags []}}

   ["models/:model_id/images/:image_id"
    {:get {:conflicting true
           :summary "Get image [fe]"
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-image-middleware]
           :swagger {:produces ["application/json" "image/jpeg"]}
           :parameters {:path {
                               ;:id s/Uuid

                               :pool_id s/Uuid
                               :model_id s/Uuid
                               :image_id s/Uuid

                               }}
           :handler get-image-thumbnail-handler
           :responses {200 {:description "OK"}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}

     :delete {:accept "application/json"
              :summary "Delete image [fe]"
              :coercion reitit.coercion.schema/coercion
              :parameters {:path {
                                  :pool_id s/Uuid
                                  :model_id s/Uuid
                                  :image_id s/Uuid
                                  }}
              :handler delete-image
              :responses {200 {:description "OK"}
                          404 {:description "Not Found"}
                          500 {:description "Internal Server Error"}}}


     }]])
