(ns leihs.inventory.server.resources.pool.models.model.images.image.constants)

(def CONTENT_NEGOTIATION_TYPE_IMAGE "image/*")
(def ALLOWED_IMAGE_CONTENT_TYPES ["image/png"
                                  "image/svg+xml"
                                  "text/html" ;; FF
                                  "image/jpeg" "image/gif" "image/avif" "image/webp" "image/apng" CONTENT_NEGOTIATION_TYPE_IMAGE])
