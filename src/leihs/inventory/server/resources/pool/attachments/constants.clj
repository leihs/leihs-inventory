(ns leihs.inventory.server.resources.pool.attachments.constants)

(def ACCEPT_TYPES_ATTACHMENT
  ["application/json" "application/octet-stream" "application/pdf" "application/zip"
   "image/png" "image/jpeg" "image/jpg" "image/gif" "image/vnd.dwg"
   "text/rtf" "text/plain" "text/html"
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

(def config
  {:api
   {:upload-dir "/tmp/"
    :images {:max-size-mb 8
             :allowed-file-types ["png" "jpg" "jpeg"]
             :thumbnail {:width-px 100
                         :height-px 100}}
    :attachments {:max-size-mb 100
                  :allowed-file-types ["pdf" "zip" "png" "rtf" "gif" "plain"]}}})

(defn config-get
  "Fetch a nested config value. E.g. (config-get :api :images :thumbnail :width-px)"
  [& ks]
  (get-in config ks))
