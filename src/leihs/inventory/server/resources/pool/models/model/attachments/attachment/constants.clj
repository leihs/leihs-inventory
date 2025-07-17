(ns leihs.inventory.server.resources.pool.models.model.attachments.attachment.constants
  (:require
   [leihs.inventory.server.resources.pool.models.model.attachments.types :refer [attachment]]
   [schema.core :as s]))

(def CONTENT_DISPOSITION_INLINE_FORMATS [
                                         "text/html"
                                         "text/css"
                                         "text/plain"
                                         "text/xml"
                                         "application/xml"
                                         "application/xhtml+xml"
                                         "application/pdf"
                                         "application/json"
                                         "application/javascript"
                                         "application/x-javascript"
                                         "image/png"
                                         "image/jpeg"
                                         "image/gif"
                                         "image/webp"
                                         "image/svg+xml"
                                         "image/bmp"
                                         "image/avif"
                                         "audio/mpeg"
                                         "audio/ogg"
                                         "audio/wav"
                                         "audio/webm"
                                         "video/mp4"
                                         "video/webm"
                                         "video/ogg"
                                         ])

