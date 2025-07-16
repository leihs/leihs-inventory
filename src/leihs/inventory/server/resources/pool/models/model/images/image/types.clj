(ns leihs.inventory.server.resources.pool.models.model.images.image.types
  (:require
   ;[leihs.inventory.server.resources.pool.models.model.images.types :refer [image]]
   [schema.core :as s]))

(def delete-response
  {:status s/Str
   :image_id s/Uuid})

(def error-image-not-found
  {:status s/Str
   :message s/Str})

(def image
  {
   :content s/Str
   :width (s/maybe s/Uuid)
   :height (s/maybe s/Uuid)
   :size s/Int
   :target_id s/Uuid
   :content_type s/Str
   :thumbnail s/Bool
   :id s/Uuid
   :target_type s/Str

   })


;(def image-min
;  {
;   :id s/Uuid
;   :target_id s/Uuid
;   :content_type s/Str
;   :filename s/Str
;   :size s/Int
;   :thumbnail s/Bool
;   })