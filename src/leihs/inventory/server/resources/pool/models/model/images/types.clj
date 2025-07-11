(ns leihs.inventory.server.resources.pool.models.model.images.types
  (:require
   ;[clojure.spec.alpha :as sa]
   ;[clojure.string :as str]
   ;[leihs.inventory.server.constants :refer [fe]]
   ;
   ;[leihs.inventory.server.resources.pool.models.coercion :as mc]
   ;
   ;[leihs.inventory.server.resources.pool.models.model.images.main :refer [post-resource
   ;                                                                        index-resources]]
   ;[leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   ;[leihs.inventory.server.utils.auth.role-auth :refer [permission-by-role-and-pool]]
   ;[leihs.inventory.server.utils.auth.roles :as roles]
   ;[leihs.inventory.server.utils.coercion.core :refer [Date]]
   ;[leihs.inventory.server.utils.constants :refer [config-get]]
   ;[reitit.coercion.schema]
   ;[reitit.coercion.spec :as spec]
   ;[ring.middleware.accept]
   [schema.core :as s]))

(def image
  {
   :id s/Uuid
   :target_id s/Uuid

   :filename s/Str
   :size s/Int
   :thumbnail s/Bool
   })


(def delete-response
  {:status s/Str
   :image_id s/Uuid})

(def delete-400-response
  {:message s/Str
   })

(def get-image-response
  image)

(def get-images-response
  {:data [image]
   :pagination s/Any})
