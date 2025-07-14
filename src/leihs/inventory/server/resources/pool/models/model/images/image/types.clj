(ns leihs.inventory.server.resources.pool.models.model.images.image.types
  (:require
   [leihs.inventory.server.resources.pool.models.model.images.types :refer [image]]
   [schema.core :as s]))

(def delete-response
  {:status s/Str
   :image_id s/Uuid})

(def delete-400-response
  {:message s/Str})
