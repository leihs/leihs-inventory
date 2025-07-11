(ns leihs.inventory.server.resources.pool.models.model.images.types
  (:require
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
