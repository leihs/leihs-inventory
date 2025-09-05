(ns leihs.inventory.server.resources.pool.models.model.attachments.attachment.types
  (:require
   [leihs.inventory.server.resources.pool.models.model.attachments.types :refer [attachment]]
   [schema.core :as s]))

;; TODO
;400 -> 404
;"error": "Attachment not found"

(def get-attachment-response
  (s/->Either [attachment s/Any]))

(def error-attachment-not-found
  {:message s/Str})
