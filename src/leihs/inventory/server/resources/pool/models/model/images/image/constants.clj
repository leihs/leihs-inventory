(ns leihs.inventory.server.resources.pool.models.model.images.image.constants)

(def CONTENT_NEGOTIATION_HEADER_TYPE "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")

(def ALLOWED_IMAGE_CONTENT_TYPES ["image/png" "image/jpeg" "image/gif" CONTENT_NEGOTIATION_HEADER_TYPE])
