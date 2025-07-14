(ns leihs.inventory.server.resources.pool.models.model.images.image.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.models.model.images.image.main :refer [delete-resource get-resource]]
   [leihs.inventory.server.resources.pool.models.model.images.image.types :refer [delete-400-response delete-response]]
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
           :swagger {:produces ["application/json" "image/jpeg"]}
           :parameters {:path {:pool_id s/Uuid
                               :model_id s/Uuid
                               :image_id s/Uuid}}
           :handler get-resource
           :responses {200 {:description "OK"
                            :body s/Any}
                       ;:body get-image-response}       ;;FIXME: by content-type
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}

     :delete {:accept "application/json"
              :summary (fe "")
              :coercion reitit.coercion.schema/coercion
              :parameters {:path {:pool_id s/Uuid
                                  :model_id s/Uuid
                                  :image_id s/Uuid}}
              :handler delete-resource
              :responses {200 {:description "OK"
                               :body delete-response}
                          400 {:description "Not Found"
                               :body delete-400-response}
                          500 {:description "Internal Server Error"}}}}]])
