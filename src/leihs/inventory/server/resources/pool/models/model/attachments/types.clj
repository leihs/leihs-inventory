(ns leihs.inventory.server.resources.pool.models.model.attachments.types
  (:require
   [leihs.inventory.server.resources.types :refer [pagination]]
   [schema.core :as s]))

(def attachment
  {:id s/Uuid
   :model_id (s/maybe s/Uuid)
   :content_type s/Str
   :filename s/Str
   :size s/Num
   :item_id (s/maybe s/Uuid)
   :content (s/maybe s/Str)})

(def get-attachments-response
  {:data [attachment]
   :pagination pagination})

(def attachment-response
  {:id s/Uuid
   :model_id (s/maybe s/Uuid)
   :content_type s/Str
   :filename s/Str
   :size s/Num
   :item_id (s/maybe s/Uuid)})