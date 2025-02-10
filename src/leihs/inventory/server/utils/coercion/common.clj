(ns leihs.inventory.server.utils.coercion.common
  (:require
   [clojure.string :as str]
   [reitit.coercion.schema]
   [clojure.spec.alpha :as sa]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [ring.util.response :refer [response status]]
   [schema.core :as s]))

(sa/def ::is_package boolean?)
(sa/def ::maintenance_period int?)
(sa/def ::type string?)
(sa/def ::updated_at any?)
(sa/def ::product string?)
;(sa/def ::id uuid?) ;; UUID spec
;(sa/def :any/id any?) ;; UUID spec
(sa/def ::manufacturer any?)
(sa/def ::version string?)
(sa/def ::created_at any?)
(sa/def ::technical_detail string?)

;(sa/def ::description (sa/nilable string?))
;(sa/def ::attachments (sa/nilable any?)) ;; Optional field
;(sa/def ::rental_price (sa/nilable any?))
;(sa/def ::cover_image_id (sa/nilable any?))
;(sa/def ::hand_over_note (sa/nilable any?))
;(sa/def ::internal_description (sa/nilable string?))
;(sa/def ::info_url (sa/nilable any?))
;(sa/def ::technical_detail (sa/nilable string?))
;(sa/def ::version (sa/nilable string?))
;(sa/def ::description (sa/nilable string?))
;(sa/def ::rental_price (sa/nilable string?))
;(sa/def ::cover_image_id (sa/nilable string?))
;(sa/def ::hand_over_note (sa/nilable string?))
;(sa/def ::internal_description (sa/nilable string?))
;(sa/def ::info_url (sa/nilable string?))

;(sa/def ::id (sa/nilable uuid?))
;(sa/def ::updated_at (sa/nilable any?))
;(sa/def ::created_at (sa/nilable any?))
;(sa/def ::name (sa/nilable string?))
;(sa/def ::status_note (sa/nilable string?))
;(sa/def ::shelf (sa/nilable string?))
;(sa/def ::last_check (sa/nilable string?))
;(sa/def ::item_version (sa/nilable string?))
;(sa/def :nil2/item_version (sa/nilable any?))
;(sa/def ::retired (sa/nilable any?))
;(sa/def ::retired_reason (sa/nilable string?))
;(sa/def ::price (sa/nilable string?))
;(sa/def ::invoice_date (sa/nilable string?))
;(sa/def ::parent_id (sa/nilable uuid?))
;(sa/def ::insurance_number (sa/nilable any?))
;(sa/def ::user_name (sa/nilable any?))
;(sa/def ::supplier_id (sa/nilable any?))

(sa/def ::properties any?)
(sa/def ::needs_permission boolean?)
(sa/def ::is_incomplete boolean?)







;; Define "data" inside "fields"
(sa/def ::type string?)
(sa/def ::group string?)
(sa/def ::label string?)
(sa/def ::attribute any?)
;(sa/def ::permissions ::PermissionsSchema)
(sa/def ::permissions any?)
(sa/def ::forPackage boolean?)

;; Define "fields" schema
;(sa/def ::role (sa/nilable string?))
;(sa/def ::group (sa/nilable string?))
(sa/def ::group_default string?)
(sa/def ::target_type string?)
(sa/def ::role_default string?)
(sa/def ::target_default string?)
(sa/def ::active boolean?)
(sa/def ::label string?)
;(sa/def :bool/owner boolean?)
;(sa/def :bool/owner (sa/nilable boolean?))
;(sa/def :bool/owner (sa/nilable string?))
(sa/def ::id string?)
(sa/def ::id uuid?)
(sa/def ::position int?)
(sa/def ::target string?)
(sa/def ::owner string?)







;; Define "permissions" schema inside "data"
(sa/def ::role string?)
(sa/def ::owner ::boolean)


;; Define a UUID type (as string)
(sa/def ::uuid string?)

;; Define a nullable string
(sa/def ::nullable-string (sa/nilable string?))

;; Define boolean and integer types
(sa/def ::boolean boolean?)
(sa/def ::integer int?)

;; Define "data" schema
;(sa/def ::inventory_pool_id ::uuid)
(sa/def ::inventory_pool_id string?)
(sa/def ::inventory_pool_id any?)
(sa/def ::responsible_department ::uuid)
(sa/def ::inventory_code string?)



;; Ensure all spec keys are properly namespaced
(sa/def ::properties map?)
(sa/def ::inventory_code string?)
(sa/def ::owner_id uuid?)
(sa/def ::is_borrowable boolean?)
(sa/def ::retired inst?) ;; Date
(sa/def ::is_inventory_relevant boolean?)
(sa/def ::last_check inst?) ;; Date
(sa/def ::shelf string?)
(sa/def ::status_note string?)
;(sa/def ::name (sa/nilable string?))
;(sa/def ::invoice_number (sa/nilable string?))
(sa/def ::is_broken boolean?)
(sa/def ::note string?)
(sa/def ::updated_at inst?) ;; Date
(sa/def ::retired_reason string?)
;(sa/def ::responsible (sa/nilable string?))
;(sa/def ::invoice_date (sa/nilable inst?)) ;; Date
(sa/def ::model_id uuid?)
;(sa/def ::supplier_id (sa/nilable uuid?))
;(sa/def ::parent_id (sa/nilable uuid?))
(sa/def ::id uuid?)
(sa/def ::inventory_pool_id uuid?)
(sa/def ::is_incomplete boolean?)
;(sa/def ::item_version (sa/nilable string?))
(sa/def ::needs_permission boolean?)
(sa/def ::user_name string?)
(sa/def ::room_id uuid?)
;(sa/def ::serial_number (sa/nilable string?))
;(sa/def ::price double?)
;(sa/def ::price (sa/nilable any?))
(sa/def ::created_at inst?) ;; Date
(sa/def ::items_attributes any?) ;; Date
;(sa/def ::insurance_number (sa/nilable string?))



;; Define primitive field specs
(sa/def ::note string?)
(sa/def ::is_inventory_relevant boolean?)
;(sa/def ::last_check inst?) ;; Assuming it is a date
(sa/def ::last_check any?)
(sa/def ::user_name string?)

;(sa/def ::price int?)
(sa/def ::price string?)
;(sa/def ::price any?)

(sa/def ::shelf string?)
(sa/def ::inventory_code string?)
(sa/def ::retired boolean?)
(sa/def ::retired_reason string?)
(sa/def ::is_broken boolean?)
(sa/def ::is_incomplete boolean?)
(sa/def ::is_borrowable boolean?)
(sa/def ::status_note string?)
(sa/def ::room_id uuid?)
(sa/def ::owner_id uuid?)

;; Define the schema for items in items_attributes
(sa/def ::item_inventory_code string?)
(sa/def ::item_id uuid?)


(sa/def ::active boolean?)
(sa/def ::data any?)
(sa/def ::group string?)
(sa/def ::id string?)
(sa/def ::label string?)
;(sa/def ::owner (nil-or uuid?))
(sa/def ::owner (nil-or string?))
(sa/def ::position int?)
(sa/def ::role (nil-or string?))
(sa/def ::role_default string?)
(sa/def ::target (nil-or string?))
(sa/def ::target_default string?)

(sa/def ::inventory_code string?)
(sa/def ::inventory_pool_id uuid?)
(sa/def ::responsible any?)
;(sa/def ::responsible (sa/nilable any?))
;(sa/def ::invoice_number (sa/nilable any?))
;(sa/def ::note (sa/nilable string?))
;(sa/def ::serial_number (sa/nilable string?))

(sa/def ::responsible_department uuid?)


(sa/def ::file multipart/temp-file-part)
;(sa/def ::name (sa/nilable string?))
;(sa/def ::product (sa/nilable string?))
;(sa/def ::item_version (sa/nilable string?))
;(sa/def ::version (sa/nilable string?))
;(sa/def ::manufacturer (sa/nilable string?))
;(sa/def ::isPackage (sa/nilable string?))
;(sa/def ::description (sa/nilable string?))
;(sa/def ::technicalDetails (sa/nilable string?))
;(sa/def ::internalDescription (sa/nilable string?))
;(sa/def ::importantNotes (sa/nilable string?))
;(sa/def ::allocations (sa/nilable string?))

(sa/def ::compatible_ids (sa/or
                           :multiple (sa/or :coll (sa/coll-of uuid?)
                                       :str string?)
                           :single uuid?
                           :none nil?))

;; TODO: initial validation-error
(sa/def ::category_ids (sa/or
                         :multiple (sa/or :coll (sa/coll-of uuid?)
                                     :str string?)
                         :single uuid?
                         :none nil?))

(sa/def ::name string?)
(sa/def ::delete boolean?)
(sa/def ::position int?)
(sa/def ::id uuid?)
(sa/def ::id-or-nil (sa/nilable uuid?))
(sa/def ::name string?)
(sa/def ::created_at any?)
(sa/def ::updated_at any?)

(sa/def ::type
  (sa/and string? #{"Category"}))

;(sa/def ::category (sa/keys :opt-un [::delete ::created_at ::updated_at]
;                     :req-un [::id ::type ::name]))
;(sa/def ::categories (sa/or
;                       :single (sa/or :coll (sa/coll-of ::category)
;                                 :str string?)
;                       :none nil?))
;
;(sa/def ::compatible (sa/keys :opt-un [::delete]
;                       :req-un [::id ::product]))
;(sa/def ::compatibles (sa/or
;                        :single (sa/or :coll (sa/coll-of ::compatible)
;                                  :str string?)
;                        :none nil?))

(sa/def ::images-to-delete string?)
(sa/def ::attachments-to-delete string?)

(sa/def ::images (sa/or :multiple (sa/coll-of ::file :kind vector?)
                   :single ::file))
(sa/def ::attachments any?)
(sa/def ::entitlement_group_id uuid?)
(sa/def ::entitlement_id uuid?)
(sa/def ::quantity int?)
;(sa/def ::entitlement (sa/keys :opt-un [::name ::delete ::position]
;                        :req-un [::entitlement_group_id ::entitlement_id ::quantity]))
;(sa/def ::entitlements (sa/or
;                         :single (sa/or :coll (sa/coll-of ::entitlement)
;                                   :str string?)
;                         :none nil?))
(sa/def ::inventory_bool boolean?)
;(sa/def ::accessory (sa/keys :req-opt [::id-or-nil ::delete] :req-un [::name ::inventory_bool]))
;(sa/def ::accessories (sa/or
;                        :single (sa/or :coll (sa/coll-of ::accessory)
;                                  :str string?)
;                        :none nil?))
(sa/def ::properties string?)
(sa/def ::serial_number string?)
(sa/def ::note string?)
(sa/def ::status_note string?)

(sa/def ::owner_id uuid?)

(sa/def ::building_id uuid?)
(sa/def ::room_id uuid?)
(sa/def ::software_id uuid?)
;(sa/def ::supplier_id (sa/nilable string?))
(sa/def ::model_id uuid?)

(sa/def ::inventory_code string?)
(sa/def ::item_version string?)
(sa/def ::is_incomplete boolean?)
(sa/def ::is_broken boolean?)
(sa/def ::retired boolean?)
(sa/def ::retired_reason string?)
(sa/def ::price string?)
(sa/def ::invoice_date string?)
(sa/def ::invoice_date any?)
(sa/def ::invoice_number string?)

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
(sa/def ::properties string?)
;(sa/def ::property (sa/keys :req-opt [::id-or-nil] :req-un [::key ::value]))
