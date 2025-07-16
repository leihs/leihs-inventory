(ns leihs.inventory.server.resources.pool.models.model.attachments.attachment.types
  (:require
   [leihs.inventory.server.resources.pool.models.model.attachments.types :refer [attachment]]
   [schema.core :as s]))

;; TODO
;400 -> 404
;"error": "Attachment not found"

(def get-attachment-response
  attachment)

(def delete-response
  {:status s/Str
   :attachment_id s/Uuid})

(def error-attachment-not-found
  {:error s/Str})