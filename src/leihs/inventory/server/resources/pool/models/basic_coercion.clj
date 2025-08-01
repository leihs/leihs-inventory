(ns leihs.inventory.server.resources.pool.models.basic_coercion
  (:require
   [clojure.spec.alpha :as sa]
   [reitit.coercion.schema]
   [reitit.ring.middleware.multipart :as multipart]
   [ring.middleware.accept]))

(sa/def ::file multipart/temp-file-part)
(sa/def :nil/name (sa/nilable string?))
(sa/def :nil/product (sa/nilable string?))
(sa/def ::product string?)
(sa/def :nil/item_version (sa/nilable string?))
(sa/def :nil/version (sa/nilable string?))
(sa/def :nil/manufacturer (sa/nilable string?))
(sa/def :nil-str/is_package (sa/nilable string?))
(sa/def :nil/is_package (sa/nilable boolean?))
(sa/def ::is_package boolean?)
(sa/def :nil/description (sa/nilable string?))
(sa/def :nil/technical_detail (sa/nilable string?))
(sa/def :nil/internal_description (sa/nilable string?))
(sa/def :nil/important_notes (sa/nilable string?))
(sa/def :nil/hand_over_note (sa/nilable string?))
(sa/def :nil/allocations (sa/nilable string?))
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
(sa/def ::created_at any?)
(sa/def ::updated_at any?)
(sa/def ::type (sa/and string? #{"Category"}))
(sa/def ::category (sa/keys :opt-un [::name]
                            :req-un [::id]))
(sa/def ::categories
  (sa/coll-of ::category :kind vector? :min-count 0))
(sa/def :put-post/categories
  (sa/coll-of ::id :kind vector? :min-count 0))
(sa/def :nil/compatible (sa/keys :opt-un [::cover_image_id :nil/url]
                                 :req-un [:nil/id :nil/product]))
(sa/def ::compatible (sa/keys :opt-un [::product :nil/image_id :nil/url :nil/version]
                              :req-un [::id]))
(sa/def ::compatibles
  (sa/coll-of ::compatible :kind vector? :min-count 0))
(sa/def :min/compatible (sa/keys :opt-un [::product]
                                 :req-un [::id]))
(sa/def :min/compatibles
  (sa/coll-of :min/compatible :kind vector? :min-count 0))
(sa/def ::images_to_delete string?)
(sa/def ::attachments_to_delete string?)
(sa/def ::images (sa/or :multiple (sa/coll-of ::file :kind vector?)
                        :single ::file))
(sa/def ::image (sa/keys :opt-un [::is_cover :nil/url :upload/content_type]
                         :req-un [::id ::filename]))
(sa/def ::attachment (sa/keys :opt-un [:nil/url :upload/content_type]
                              :req-un [::id ::filename]))
(sa/def :min/images
  (sa/coll-of ::image :kind vector? :min-count 0))
(sa/def ::is_cover boolean?)
(sa/def ::filename string?)
(sa/def ::attachments
  (sa/coll-of ::attachment :kind vector? :min-count 0))
(sa/def ::entitlement_group_id uuid?)
(sa/def :entitlement/group_id uuid?)
(sa/def ::entitlement_id uuid?)
(sa/def :nil/entitlement_id (sa/nilable uuid?))
(sa/def ::quantity int?)
(sa/def :json/entitlement (sa/keys :opt-un [::name ::position :nil/id]
                                   :req-un [:entitlement/group_id
                                            ::quantity]))
(sa/def ::entitlements
  (sa/coll-of :json/entitlement :kind vector? :min-count 0))
(sa/def ::inventory_bool boolean?)
(sa/def ::has_inventory_pool boolean?)
(sa/def ::accessory (sa/keys :req-un [::name] :opt-un [:nil/id ::delete ::has_inventory_pool] :kind map?))
(sa/def ::accessories (sa/coll-of ::accessory))
(sa/def ::serial_number string?)
(sa/def :nil/note (sa/nilable string?))
(sa/def ::status_note string?)
(sa/def ::owner_id uuid?)
(sa/def ::building_id uuid?)
(sa/def ::room_id uuid?)
(sa/def ::software_id uuid?)
(sa/def ::supplier_id (sa/nilable string?))
(sa/def :nil-any/supplier_id (sa/nilable any?))
(sa/def ::model_id uuid?)
(sa/def ::inventory_code string?)
(sa/def ::item_version string?)
(sa/def ::is_incomplete boolean?)
(sa/def ::is_broken boolean?)
(sa/def ::retired boolean?)
(sa/def ::retired_reason string?)
(sa/def ::price string?)
(sa/def ::invoice_date string?)
(sa/def :any/invoice_date any?)
(sa/def ::invoice_number string?)
(sa/def :nil/invoice_number (sa/nilable string?))
(sa/def ::shelf string?) ;; FIXME
(sa/def ::user_name string?)
(sa/def ::activation_type string?)
(sa/def ::dongle_id string?)
(sa/def ::license_type string?)
(sa/def ::total_quantity string?)
(sa/def ::operating_system string?)
(sa/def ::quantity_allocations any?)
(sa/def ::maintenance_currency string?)
(sa/def ::license_expiration string?)
(sa/def ::installation string?)
(sa/def ::p4u string?)
(sa/def ::reference string?)
(sa/def ::project_number string?)
(sa/def ::procured_by string?)
(sa/def ::maintenance_contract string?)
(sa/def ::maintenance_expiration string?)
(sa/def ::maintenance_price string?)
(sa/def ::key string?)
(sa/def ::value string?)
(sa/def :simple/properties string?)
(sa/def ::property (sa/keys :opt-un [:nil/id] :req-un [::key ::value]))
(sa/def ::inventory_pool_id uuid?)
(sa/def ::responsible any?)
(sa/def :nil/responsible (sa/nilable any?))
(sa/def :nil/invoice_number (sa/nilable any?))
(sa/def :nil/serial_number (sa/nilable string?))
(sa/def ::responsible_department uuid?)
(defn nil-or [pred]
  (sa/or :nil nil? :value pred))
(sa/def ::active boolean?)
(sa/def ::data any?)
(sa/def ::group string?)
(sa/def :str/id string?)
(sa/def ::label string?)
(sa/def :nil/owner (nil-or string?))
(sa/def ::role (nil-or string?))
(sa/def ::role_default string?)
(sa/def ::target (nil-or string?))
(sa/def ::target_default string?)
(sa/def ::note string?)
(sa/def ::is_inventory_relevant boolean?)
(sa/def ::last_check any?)
(sa/def :nil/user_name (sa/nilable string?))
(sa/def :any/price any?)
(sa/def ::item_inventory_code string?)
(sa/def ::item_id uuid?)
(sa/def :any/items_attributes any?)
(sa/def ::items_attributes
  (sa/coll-of (sa/keys :req-un [::item_inventory_code ::item_id]) :kind vector?))
(sa/def :res/properties map?)
(sa/def :res/inventory_code string?)
(sa/def :res/owner_id uuid?)
(sa/def :res/is_borrowable boolean?)
(sa/def :res/retired inst?) ;; Date
(sa/def :res/is_inventory_relevant boolean?)
(sa/def :res/last_check inst?) ;; Date
(sa/def :res/shelf string?)
(sa/def :res/status_note string?)
(sa/def :res/name (sa/nilable string?))
(sa/def :res/invoice_number (sa/nilable string?))
(sa/def :res/is_broken boolean?)
(sa/def :res/note string?)
(sa/def :res/updated_at inst?) ;; Date
(sa/def :res/retired_reason string?)
(sa/def :res/responsible (sa/nilable string?))
(sa/def :res/invoice_date (sa/nilable inst?)) ;; Date
(sa/def :res/model_id uuid?)
(sa/def :res/supplier_id (sa/nilable uuid?))
(sa/def :res/parent_id (sa/nilable uuid?))
(sa/def :res/id uuid?)
(sa/def :res/inventory_pool_id uuid?)
(sa/def :res/is_incomplete boolean?)
(sa/def :res/item_version (sa/nilable string?))
(sa/def :res/needs_permission boolean?)
(sa/def :res/user_name string?)
(sa/def :res/room_id uuid?)
(sa/def :res/serial_number (sa/nilable string?))
(sa/def :res/price (sa/nilable any?))
(sa/def :res/created_at inst?) ;; Date
(sa/def :res/items_attributes any?) ;; Date
(sa/def :res/insurance_number (sa/nilable string?))
(sa/def ::uuid string?)
(sa/def ::nullable-string (sa/nilable string?))
(sa/def ::integer int?)
(sa/def ::responsible_department ::uuid)
(sa/def ::DataSchema
  (sa/keys :req-un [::inventory_pool_id
                    ::responsible_department
                    ::inventory_code]))
(sa/def ::PermissionsSchema
  (sa/keys :req-un [::role ::owner]))
(sa/def :model/type string?)
(sa/def ::attribute any?)
(sa/def ::permissions any?)
(sa/def ::forPackage boolean?)
(sa/def :nil/role (sa/nilable string?))
(sa/def :nil/group (sa/nilable string?))
(sa/def :nil/group_default (sa/nilable string?))
(sa/def :nil/role_default (sa/nilable string?))
(sa/def ::group_default string?)
(sa/def ::target_type string?)
(sa/def :nil-str/owner (sa/nilable string?))
(sa/def ::owner ::nullable-string) ;; "true" is string, but could be coerced to boolean
(sa/def :nil/id (sa/nilable uuid?))
(sa/def :nil/updated_at (sa/nilable any?))
(sa/def :nil/created_at (sa/nilable any?))
(sa/def :nil/shelf (sa/nilable string?))
(sa/def :nil-str/shelf (sa/nilable string?))
(sa/def :nil/last_check (sa/nilable any?))
(sa/def :nil2/item_version (sa/nilable any?))
(sa/def :nil/retired (sa/nilable any?))
(sa/def :nil/retired_reason (sa/nilable string?))
(sa/def :nil/price (sa/nilable string?))
(sa/def :nil/invoice_date (sa/nilable any?))
(sa/def ::properties any?)
(sa/def :nil/parent_id (sa/nilable uuid?))
(sa/def :nil/insurance_number (sa/nilable any?))
(sa/def :nil-any/user_name (sa/nilable any?))
(sa/def :nil/supplier_id (sa/nilable any?))
(sa/def ::needs_permission boolean?)
(sa/def :nil/attachments (sa/nilable any?)) ;; Optional field
(sa/def :nil/rental_price (sa/nilable any?))
(sa/def ::manufacturer any?)
(sa/def ::version string?)
(sa/def ::technical_detail string?)
(sa/def :any/id any?) ;; UUID spec
(sa/def :nil/info_url (sa/nilable string?))

(sa/def ::id uuid?)
(sa/def ::model_id uuid?)
(sa/def ::product string?)
(sa/def ::filename string?)
(sa/def ::manufacturer string?)
(sa/def ::size int?)
