(ns leihs.inventory.server.resources.pool.models.basic-coercion
  (:require
   [clojure.spec.alpha :as sa]
   [reitit.coercion.schema]
   [reitit.ring.middleware.multipart :as multipart]
   [ring.middleware.accept]))

(sa/def ::file multipart/temp-file-part)
(sa/def ::product string?)
(sa/def ::is_package boolean?)
(sa/def ::name string?)
(sa/def :image/id any?)
(sa/def :image/is_cover any?)
(sa/def :image/filename string?)
(sa/def :upload/content_type string?)
(sa/def :image/url string?)
(sa/def :image/thumbnail_url string?)
(sa/def ::id uuid?)
(sa/def ::category (sa/keys :opt-un [::name] :req-un [::id]))
(sa/def ::compatible (sa/keys :opt-un [::product :nil/image_id :nil/url :nil/version] :req-un [::id]))
(sa/def ::image (sa/keys :opt-un [::is_cover :nil/url :upload/content_type] :req-un [::id ::filename]))
(sa/def ::attachment (sa/keys :opt-un [:nil/url :upload/content_type] :req-un [::id ::filename]))
(sa/def ::is_cover boolean?)
(sa/def ::is_deletable boolean?)
(sa/def ::filename string?)
(sa/def ::nullable-string (sa/nilable string?))
(sa/def ::owner ::nullable-string)
