(ns leihs.inventory.server.resources.pool.attachments.constants)

(def ACCEPT_TYPES_ATTACHMENT
  ["application/json" "application/octet-stream" "application/pdf" "application/zip"
   "image/png" "image/jpeg" "image/jpg" "image/gif" "image/vnd.dwg"
   "text/rtf" "text/plain"
   "*/*"])

(def CONTENT_DISPOSITION_INLINE_FORMATS ["text/html"
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
                                         "video/ogg"])
