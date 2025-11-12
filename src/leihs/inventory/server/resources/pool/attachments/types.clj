(ns leihs.inventory.server.resources.pool.attachments.types
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

(def get-attachment-response
  (s/->Either [attachment s/Any]))

(def error-attachment-not-found
  (s/->Either [{:message s/Str} s/Any]))

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
