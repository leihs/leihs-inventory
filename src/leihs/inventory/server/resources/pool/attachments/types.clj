(ns leihs.inventory.server.resources.pool.attachments.types
  (:require
   [leihs.inventory.server.resources.pool.models.model.attachments.types :refer [attachment]]
   [schema.core :as s]))

(def get-attachment-response
  (s/->Either [attachment s/Any]))

(def error-attachment-not-found
  (s/->Either [{:message s/Str} s/Any]))
