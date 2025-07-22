(ns leihs.inventory.server.resources.pool.models.model.images.types
  (:require
   [leihs.inventory.server.resources.types :refer [pagination]]
   [schema.core :as s]))

(def image
  {:id s/Uuid
   :target_id s/Uuid
   :content_type s/Str
   :filename s/Str
   :size s/Int
   :thumbnail s/Bool})

(def post-response {:image image
                    :thumbnail image
                    :model_id s/Uuid})

(def get-images-response
  (s/->Either [{:data [image]
   :pagination pagination} [image]]))
