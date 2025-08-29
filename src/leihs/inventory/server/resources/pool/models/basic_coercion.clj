(ns leihs.inventory.server.resources.pool.models.basic-coercion
  (:require
   [clojure.spec.alpha :as sa]
   [reitit.coercion.schema]
   [ring.middleware.accept]))

(sa/def ::product string?)
(sa/def :nil/version (sa/nilable string?))
(sa/def :nil/manufacturer (sa/nilable string?))
(sa/def ::is_package boolean?)
(sa/def :nil/description (sa/nilable string?))
(sa/def :nil/technical_detail (sa/nilable string?))
(sa/def :nil/internal_description (sa/nilable string?))
(sa/def :model/type string?)
(sa/def :nil/hand_over_note (sa/nilable string?))
(sa/def ::name string?)
(sa/def ::delete boolean?)
(sa/def :nil/cover_image_id (sa/nilable uuid?))
(sa/def :nil/image_id (sa/nilable uuid?))
(sa/def :image/id any?)
(sa/def :image/is_cover any?)
(sa/def :image/filename string?)
(sa/def :upload/content_type string?)
(sa/def :image/url string?)
(sa/def :image/thumbnail_url string?)
(sa/def :image/to_delete any?)
(sa/def :nil/url (sa/nilable string?))
(sa/def ::position int?)
(sa/def ::id uuid?)
(sa/def :models/type (sa/and string? #{"Models" "Software"}))
(sa/def ::category (sa/keys :opt-un [::name]
                            :req-un [::id]))
(sa/def ::categories
  (sa/coll-of ::category :kind vector? :min-count 0))
(sa/def ::compatible (sa/keys :opt-un [::product :nil/image_id :nil/url :nil/version]
                              :req-un [::id]))
(sa/def ::compatibles
  (sa/coll-of ::compatible :kind vector? :min-count 0))
(sa/def :min/compatible (sa/keys :opt-un [::product]
                                 :req-un [::id]))
(sa/def :min/compatibles
  (sa/coll-of :min/compatible :kind vector? :min-count 0))
(sa/def ::image (sa/keys :opt-un [::is_cover :nil/url :upload/content_type]
                         :req-un [::id ::filename]))
(sa/def ::attachment (sa/keys :opt-un [:nil/url :upload/content_type]
                              :req-un [::id ::filename]))
(sa/def :min/images
  (sa/coll-of ::image :kind vector? :min-count 0))
(sa/def ::is_cover boolean?)
(sa/def ::is_deletable boolean?)
(sa/def ::filename string?)
(sa/def ::attachments
  (sa/coll-of ::attachment :kind vector? :min-count 0))
(sa/def :entitlement/group_id uuid?)
(sa/def ::quantity int?)
(sa/def :json/entitlement (sa/keys :opt-un [::name ::position :nil/id]
                                   :req-un [:entitlement/group_id
                                            ::quantity]))
(sa/def ::entitlements
  (sa/coll-of :json/entitlement :kind vector? :min-count 0))
(sa/def ::has_inventory_pool boolean?)
(sa/def ::accessory (sa/keys :req-un [::name] :opt-un [:nil/id ::delete ::has_inventory_pool]))
(sa/def ::accessories (sa/coll-of ::accessory))
(sa/def ::model_id uuid?)
(sa/def ::inventory_code string?)
(sa/def ::key string?)
(sa/def ::value string?)
(sa/def ::property (sa/keys :opt-un [:nil/id] :req-un [::key ::value]))
(defn non-neg-number? [x]
  (and (number? x) (not (neg? x))))
(sa/def ::owner (sa/nilable string?))
(sa/def :nil/id (sa/nilable uuid?))
(sa/def :nil-pos-number/price (sa/nilable non-neg-number?))
(sa/def ::properties any?)
(sa/def ::manufacturer any?)
(sa/def ::technical_detail string?)
(sa/def ::filename string?)
(sa/def ::total_rows int?)
(sa/def ::total_pages int?)
(sa/def ::size pos-int?)
(sa/def ::page pos-int?)
(sa/def ::pagination
  (sa/keys :req-un [::total_rows
                    ::total_pages
                    ::page
                    ::size]))
