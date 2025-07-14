(ns leihs.inventory.server.resources.pool.models.model.images.types
  (:require
   [schema.core :as s]))

(def image
  {:id s/Uuid
   :target_id s/Uuid

   :filename s/Str
   :size s/Int
   :thumbnail s/Bool})

(def get-images-response
  {:data [image]
   :pagination s/Any})
