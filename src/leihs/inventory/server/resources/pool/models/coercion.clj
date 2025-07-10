(ns leihs.inventory.server.resources.pool.models.coercion
  (:require
   [clojure.spec.alpha :as sa]
   [leihs.core.core :refer [presence]]
   [leihs.inventory.server.utils.auth.roles :as roles]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [reitit.ring.middleware.multipart :as multipart]
   [ring.middleware.accept]
   [ring.util.response :as response]
   [schema.core :as s]
   [spec-tools.core :as st]
   [spec-tools.data-spec :as ds]))

(def models-response-payload
  (merge
    {:id (s/cond-pre s/Uuid s/Str)
     (s/optional-key :type) (s/maybe s/Str)
     (s/optional-key :manufacturer) (s/maybe s/Str)
     (s/optional-key :product) (s/maybe s/Str)
     (s/optional-key :version) (s/maybe s/Str)
     (s/optional-key :info_url) (s/maybe s/Str)
     (s/optional-key :rental_price) (s/maybe s/Any)
     (s/optional-key :maintenance_period) (s/maybe s/Int)
     (s/optional-key :is_package) (s/maybe s/Bool)
     (s/optional-key :hand_over_note) (s/maybe s/Str)
     (s/optional-key :description) (s/maybe s/Str)
     (s/optional-key :internal_description) (s/maybe s/Str)
     (s/optional-key :technical_detail) (s/maybe s/Str)
     (s/optional-key :created_at) (s/maybe s/Any)
     (s/optional-key :updated_at) (s/maybe s/Any)
     (s/optional-key :cover_image_id) (s/maybe s/Any)
     (s/optional-key :image_url) (s/maybe s/Str)
     (s/optional-key :thumbnail_url) (s/maybe s/Str)}
    {s/Keyword s/Any})
) ; <-- allows extra keys!

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Definition by def
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def models-request-payload
  {:type s/Str
   :product s/Str
   (s/optional-key :manufacturer) (s/maybe s/Str)})

(def item-response-post
  {:data :item/response
   :validation [any?]})

(def item-data-schema
  {:inventory_pool_id uuid?
   :responsible_department string?
   :quantity int?
   :inventory_code string?})

(def item-response-get
  {:data item-data-schema
   :fields [:items/response]})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Definition by sa/def
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(sa/def :items/response
  (sa/keys :req-un [:nil/role
                    :nil/group
                    ::active
                    ::label
                    :str/id
                    ::position
                    :nil-str/owner
                    ::data]
           :opt-un [:nil/group_default
                    :nil/role_default
                    ::target_default]))

(sa/def :software/response
  (sa/keys :req-un [:nil/description
                    ::is_package
                    ::type
                    :nil/hand_over_note
                    :nil/internal_description
                    ::product
                    ::id
                    ::manufacturer
                    :nil/version
                    :nil/technical_detail]

           :opt-un [::attachments
                    ::maintenance_period
                    :nil/rental_price
                    :nil/cover_image_id
                    ::updated_at
                    :nil/info_url
                    ::created_at]))

(sa/def ::file multipart/temp-file-part)
(sa/def ::name (sa/nilable string?))
(sa/def :nil/product (sa/nilable string?))
(sa/def ::product string?)
(sa/def ::item_version (sa/nilable string?))
(sa/def ::version (sa/nilable string?))
(sa/def ::manufacturer (sa/nilable string?))
(sa/def :str/is_package (sa/nilable string?))
(sa/def ::description (sa/nilable string?))
(sa/def ::technical_detail (sa/nilable string?))
(sa/def ::internal_description (sa/nilable string?))
(sa/def ::important_notes (sa/nilable string?))
(sa/def ::hand_over_note (sa/nilable string?))
(sa/def ::allocations (sa/nilable string?))

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
(sa/def ::cover_image_id (sa/nilable uuid?))
(sa/def ::image_url (sa/nilable string?))
(sa/def ::thumbnail_url (sa/nilable string?))
(sa/def ::position int?)
(sa/def ::id uuid?)
(sa/def ::name string?)
(sa/def ::created_at any?)
(sa/def ::updated_at any?)

(sa/def ::type
  (sa/and string? #{"Category"}))

(sa/def ::category (sa/keys :opt-un []
                            ;:req-un [::id ::type ::name]))
                            :req-un [::id ::name]))
(sa/def ::categories (sa/or
                      :single (sa/or :coll (sa/coll-of ::category)
                                     :str string?)
                      :none nil?))

(sa/def :put-post/categories (sa/or
                              :single (sa/or :coll (sa/coll-of ::id)
                                             :str string?)
                              :none nil?))
(sa/def :nil/compatible (sa/keys :opt-un [::cover_image_id ::image_url ::thumbnail_url]
                                 :req-un [:nil/id :nil/product]))

(sa/def ::compatible (sa/keys :opt-un [::cover_image_id ::image_url ::thumbnail_url]
                              :req-un [::id ::product]))

(sa/def ::compatibles (sa/or
                       :single (sa/or :coll (sa/coll-of ::compatible)
                                      :str string?)
                       :none nil?))
(sa/def :put-post/compatibles (sa/or
                               :single (sa/or :coll (sa/coll-of ::id)
                                              :str string?)
                               :none nil?))
(sa/def ::images_to_delete string?)
(sa/def ::attachments_to_delete string?)
(sa/def ::images (sa/or :multiple (sa/coll-of ::file :kind vector?)
                        :single ::file))
(sa/def :min/images (sa/or :multiple (sa/coll-of any? :kind vector?)
                           :single any?))
(sa/def ::attachments any?)
(sa/def ::entitlement_group_id uuid?)
(sa/def :entitlement/group_id uuid?)
(sa/def ::entitlement_id uuid?)
(sa/def :nil/entitlement_id (sa/nilable uuid?))
(sa/def ::quantity int?)
(sa/def :json/entitlement (sa/keys :opt-un [::name ::position :nil/id]
                                   :req-un [:entitlement/group_id
                                            ::quantity]))
(sa/def ::entitlements (sa/or
                        :single (sa/or :coll (sa/coll-of :json/entitlement)
                                       :str string?)
                        :none nil?))
(sa/def ::inventory_bool boolean?)
(sa/def ::has_inventory_pool boolean?)
(sa/def ::accessory (sa/keys :req-un [::name] :opt-un [::id ::delete ::has_inventory_pool] :kind map?))
(sa/def ::accessories (sa/or :coll (sa/coll-of ::accessory) :kind vector? :str string?)) ;; TODO: cleanup, remove :str definition [fe]

(sa/def ::properties string?)
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
(sa/def ::property (sa/keys :req-opt [::id-or-nil] :req-un [::key ::value]))
(sa/def ::image_attribute (sa/keys :req-opt [:image/filename
                                             :image/content_type
                                             :image/url
                                             :image/to_delete
                                             :image/thumbnail_url] :req-un [:image/id :image/is_cover]))

(sa/def :license/properties (sa/keys :req-opt [::activation_type
                                               ::dongle_id
                                               ::license_type
                                               ::total_quantity
                                               ::license_expiration
                                               ::p4u
                                               ::reference
                                               ::project_number
                                               ::procured_by
                                               ::maintenance_contract
                                               ::maintenance_expiration
                                               ::maintenance_price] :req-un []))

(sa/def :license/multipart (sa/keys :opt-un [::model_id
                                             ::supplier_id
                                             ::attachments_to_delete
                                             ::attachments
                                             ::retired_reason
                                             :simple/properties
                                             ::owner_id
                                             ::item_version]
                                    :req-un [::serial_number
                                             ::note
                                             ::invoice_date
                                             ::price
                                             ::retired
                                             ::is_borrowable
                                             ::inventory_code]))

(sa/def :item/multipart (sa/keys :opt-un [::model_id
                                          ::supplier_id
                                          ::attachments_to_delete
                                          ::attachments
                                          ::retired_reason
                                          ::quantity ;; TODO: used for POST only
                                          ::owner_id
                                          ::user_name]
                                 :req-un [::serial_number
                                          ::note
                                          ::invoice_date
                                          ::invoice_number
                                          ::price
                                          ::shelf
                                          ::inventory_code
                                          ::retired
                                          ::is_borrowable
                                          ::is_broken
                                          ::is_incomplete
                                          ::room_id
                                          ::status_note
                                          ::properties]))

(sa/def :package/multipart (sa/keys :opt-un [::model_id
                                             ::supplier_id
                                             ::attachments_to_delete
                                             ::attachments
                                             ::retired_reason
                                             ::owner_id
                                             ::user_name]
                                    :req-un [::note
                                             ::price
                                             ::shelf
                                             ::inventory_code
                                             ::retired
                                             ::is_borrowable
                                             ::is_broken
                                             ::is_incomplete
                                             ::room_id
                                             ::status_note
                                             ::properties]))

(sa/def ::inventory_code string?)
(sa/def ::inventory_pool_id uuid?)
(sa/def ::responsible any?)
(sa/def :nil/responsible (sa/nilable any?))
(sa/def :nil/invoice_number (sa/nilable any?))
(sa/def :nil/note (sa/nilable string?))
(sa/def :nil/serial_number (sa/nilable string?))
(sa/def ::responsible_department uuid?)

(sa/def ::data-spec
  (st/spec {:spec (sa/keys :req-un [::inventory_code
                                    ::inventory_pool_id
                                    ::responsible_department])
            :description "Data section of the body"}))

(defn nil-or [pred]
  (sa/or :nil nil? :value pred))

(sa/def ::active boolean?)
(sa/def ::data any?)
(sa/def ::group string?)
(sa/def :str/id string?)
(sa/def ::label string?)
(sa/def ::owner (nil-or string?))
(sa/def ::position int?)
(sa/def ::role (nil-or string?))
(sa/def ::role_default string?)
(sa/def ::target (nil-or string?))
(sa/def ::target_default string?)

(sa/def ::fields-spec
  (st/spec {:spec (sa/keys :req-un [::active
                                    ::data
                                    ::id
                                    ::label
                                    ::owner
                                    ::position
                                    ::role
                                    ::role_default
                                    ::target_default]
                           :opt-un [::group ::target])
            :description "Fields section of the body"}))

(sa/def :get-package-response/body-spec
  (st/spec {:spec (sa/keys :req-un [::data-spec ::fields-spec])
            :description "Body of the request"}))

(sa/def ::note string?)
(sa/def ::is_inventory_relevant boolean?)
(sa/def ::last_check any?)
(sa/def ::user_name string?)
(sa/def :nil/user_name (sa/nilable string?))
(sa/def ::price string?)
(sa/def :any/price any?)
(sa/def ::shelf string?)
(sa/def ::inventory_code string?)
(sa/def ::retired boolean?)
(sa/def ::retired_reason string?)
(sa/def ::is_broken boolean?)
(sa/def ::is_incomplete boolean?)
(sa/def ::is_borrowable boolean?)
(sa/def ::status_note string?)
(sa/def :nil/status_note (sa/nilable string?))
(sa/def ::room_id uuid?)
(sa/def ::owner_id uuid?)
(sa/def ::item_inventory_code string?)
(sa/def ::item_id uuid?)
(sa/def :any/items_attributes any?)

(sa/def ::items_attributes
  (sa/coll-of (sa/keys :req-un [::item_inventory_code ::item_id]) :kind vector?))

(sa/def :package-put/inventory-attributes
  (st/spec {:spec (sa/keys :req-un [::is_inventory_relevant
                                    ::last_check
                                    ::user_name
                                    ::price
                                    ::shelf
                                    ::inventory_code
                                    ::retired
                                    ::is_broken
                                    ::is_incomplete
                                    ::is_borrowable
                                    ::status_note
                                    ::room_id
                                    ::model_id
                                    ::owner_id]
                           :opt-un [::note ::retired_reason
                                    ::items_attributes])
            :description "Inventory attributes with details"}))

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

(sa/def :res/data
  (st/spec {:spec (sa/keys :req-un [:res/inventory_code
                                    :nil/retired
                                    :res/is_borrowable
                                    :res/is_inventory_relevant
                                    :res/is_broken
                                    :res/is_incomplete
                                    :nil/last_check
                                    :nil/shelf
                                    :nil/status_note
                                    :nil/user_name
                                    :res/room_id
                                    :res/model_id
                                    :res/owner_id
                                    :res/price]

                           :opt-un [:res/note
                                    :res/items_attributes
                                    :res/name
                                    :res/invoice_number
                                    :res/properties
                                    :res/updated_at
                                    :nil/retired_reason
                                    :nil/note
                                    :res/responsible
                                    :res/invoice_date
                                    :res/supplier_id
                                    :res/parent_id
                                    :res/id
                                    :res/inventory_pool_id
                                    :res/item_version
                                    :res/needs_permission
                                    :res/serial_number
                                    :res/created_at
                                    :res/insurance_number
                                    :res/insurance_number])

            :description "Inventory item data"}))

(sa/def :res/validation (sa/coll-of map? :kind vector?))
;; FIXME
;(sa/def :package-put-response/inventory-item
;  (st/spec {:spec (sa/keys :req-un [:res/data]
;                           :opt-un [:res/validation :res/items_attributes])
;            :description "Complete inventory response"}))

(sa/def :model-get-put-response/inventory-model
  (st/spec {:spec (sa/keys :req-un [::properties
                                    ::is_package
                                    ::accessories
                                    ::entitlements
                                    ::attachments
                                    :model/type
                                    ::categories
                                    ::id
                                    ::compatibles]
                           :opt-un [:min/images
                                    ::hand_over_note
                                    ::internal_description
                                    ::product

                                    ])
            :description "Complete inventory response"}))

(sa/def :model-optional-response/inventory-model
  (st/spec {:spec (sa/keys :req-un [::is_package
                                    ::product
                                    ::id
                                    ]
                           :opt-un [::properties
                                    ::description
                                    ::hand_over_note
                                    ::manufacturer
                                    ::version
                                    ::technical_detail
                                    ::internal_description
                                    ::accessories
                                    ::entitlements
                                    ::attachments
                                    ::cover_image_id
                                    ::categories
                                    :model2/image_attributes
                                    ::compatibles])
            :description "Complete inventory response"}))

(sa/def :model-strict-response/inventory-models (sa/or :multiple (sa/coll-of :model-get-put-response/inventory-model :kind vector?)
                                                       :single :model-get-put-response/inventory-model))

(sa/def :model-optional-response/inventory-models (sa/or :multiple (sa/coll-of :model-optional-response/inventory-model :kind vector?)
                                                         :single :model-optional-response/inventory-model))

(sa/def :package-put-response2/inventory-item
  (st/spec {:spec (sa/keys :req-un [:res/data]
                           :opt-un [:res/validation])
            :description "Complete inventory response"}))

(sa/def :software/properties (sa/or
                              :single (sa/or :coll (sa/coll-of ::property)
                                             :str string?)
                              :none nil?))

(sa/def :model/image_attributes (sa/or
                                 :single (sa/or :coll (sa/coll-of ::image_attribute)
                                                :str string?)
                                 :none nil?))

(sa/def :model2/image_attributes
  (sa/or :multiple (sa/or :coll (sa/coll-of ::image_attribute)
                          :str string?)
         :none empty?))

(sa/def :option/multipart (sa/keys :req-un [::product
                                            ::inventory_code]
                                   :opt-un [::version
                                            ::price]))

(sa/def :software/multipart (sa/keys :req-un [::product]
                                     :opt-un [::version
                                              ::manufacturer
                                              ::is_package
                                              ::description
                                              ::technical_detail
                                              ::internal_description
                                              ::hand_over_note
                                              ::categories
                                              ::attachments_to_delete
                                              ::images_to_delete
                                              :model/image_attributes
                                              ::owner
                                              ::compatibles
                                              ::images
                                              ::attachments
                                              ::entitlements
                                              :software/properties
                                              ::accessories]))

(sa/def :model/multipart (sa/keys :req-un [::product]
                                  :opt-un [::version
                                           ::manufacturer
                                           ::is_package
                                           ::description
                                           ::technical_detail
                                           ::internal_description
                                           ::hand_over_note
                                           ::categories
                                           ::owner
                                           ::compatibles
                                           ::entitlements
                                           :software/properties
                                           ::accessories]))

(defn nil-or [pred]
  (sa/or :nil nil? :value pred))

(def response-option-object {:id uuid?
                             :inventory_pool_id uuid?
                             :inventory_code string?
                             :manufacturer any?
                             :product string?
                             :version (sa/nilable string?)
                             :price (sa/nilable any?)})

(def response-option [response-option-object])

;(def FieldDataSchema ;; FIXME
;  {:type string?
;   :group string?
;   :label string?
;   (ds/opt :values) any?
;   (ds/opt :default) any?
;   :attribute any?
;   (ds/opt :forPackage) any?
;   (ds/opt :target_type) any?
;   (ds/opt :values_label_method) any?
;   (ds/opt :values_dependency_field_id) any?
;   (ds/opt :required) any?
;   :permissions {:role string?
;                 :owner boolean?}})

(sa/def :license/data-schema
  (sa/keys :req-un [::inventory_pool_id ::responsible_department ::inventory_code]))

(sa/def :license/field-schema
  (sa/keys :req-un [:nil/role
                    :nil/group
                    ::group_default
                    ::role_default
                    ::target_default
                    ::active
                    ::label
                    :any/id
                    ::position
                    ::target
                    :nil-any/owner
                    ::data]))

(sa/def ::uuid string?)
(sa/def ::nullable-string (sa/nilable string?))
(sa/def ::integer int?)
(sa/def ::inventory_pool_id string?)
(sa/def ::inventory_pool_id any?)
(sa/def ::responsible_department ::uuid)
(sa/def ::inventory_code string?)

(sa/def ::DataSchema
  (sa/keys :req-un [::inventory_pool_id
                    ::responsible_department
                    ::inventory_code]))

(sa/def ::role string?)

(sa/def ::PermissionsSchema
  (sa/keys :req-un [::role ::owner]))

(sa/def ::type string?)
(sa/def :model/type string?)
(sa/def ::group string?)
(sa/def ::label string?)
(sa/def ::attribute any?)
(sa/def ::permissions any?)
(sa/def ::forPackage boolean?)
(sa/def :nil/role (sa/nilable string?))
(sa/def :nil/group (sa/nilable string?))
(sa/def :nil/group_default (sa/nilable string?))
(sa/def :nil/role_default (sa/nilable string?))
(sa/def ::group_default string?)
(sa/def ::target_type string?)
(sa/def ::role_default string?)
(sa/def ::target_default string?)
(sa/def ::label string?)
(sa/def :nil-str/owner (sa/nilable string?))
(sa/def :str/id string?)
(sa/def ::position ::integer)
(sa/def ::target ::nullable-string)
(sa/def ::owner ::nullable-string) ;; "true" is string, but could be coerced to boolean

(sa/def :nil/id (sa/nilable uuid?))
(sa/def :nil/product (sa/nilable string?))
(sa/def :nil/updated_at (sa/nilable any?))
(sa/def :nil/created_at (sa/nilable any?))
(sa/def :nil/name (sa/nilable string?))
(sa/def :nil/status_note (sa/nilable string?))
(sa/def :nil/shelf (sa/nilable string?))
(sa/def :nil-str/shelf (sa/nilable string?))
(sa/def :nil/last_check (sa/nilable any?))
(sa/def :nil/item_version (sa/nilable string?))
(sa/def :nil2/item_version (sa/nilable any?))
(sa/def :nil/retired (sa/nilable any?))
(sa/def :nil/retired_reason (sa/nilable string?))
(sa/def :nil/price (sa/nilable string?))
(sa/def :nil/invoice_date (sa/nilable any?))
(sa/def ::properties any?)
(sa/def :nil/parent_id (sa/nilable uuid?))
(sa/def :nil/insurance_number (sa/nilable any?))
(sa/def :nil/user_name (sa/nilable string?))
(sa/def :nil-any/user_name (sa/nilable any?))
(sa/def :nil/supplier_id (sa/nilable any?))
(sa/def ::needs_permission boolean?)
(sa/def ::is_incomplete boolean?)

(sa/def :item/response
  (sa/keys :req-un [::inventory_code
                    ::owner_id
                    ::is_borrowable
                    :nil/retired
                    ::is_inventory_relevant
                    ::last_check
                    ::shelf
                    :nil/status_note
                    :nil/name
                    :nil/invoice_number
                    ::is_broken
                    :nil/note
                    :nil/updated_at
                    :nil/retired_reason
                    :nil/responsible
                    :nil/invoice_date
                    ::model_id
                    ;::supplier_id
                    :nil-any/supplier_id
                    :nil/parent_id
                    :nil/id
                    ::inventory_pool_id
                    ::is_incomplete
                    ::needs_permission
                    :nil/user_name
                    ::room_id
                    ::serial_number
                    :nil/price
                    :nil/created_at
                    :nil/insurance_number
                    ::properties]
           :opt-un [:nil2/item_version]))

(sa/def ::is_package boolean?)
(sa/def :bool/owner (sa/nilable boolean?))
(sa/def :nil-any/owner (sa/nilable any?))
(sa/def ::maintenance_period int?)
(sa/def ::type string?)
(sa/def :nil/attachments (sa/nilable any?)) ;; Optional field
(sa/def :nil/rental_price (sa/nilable any?))
(sa/def :nil/cover_image_id (sa/nilable any?))

(sa/def ::updated_at any?)
(sa/def :image/id any?)
(sa/def :image/is_cover (sa/nilable boolean?))
(sa/def :image/filename (sa/nilable string?))
(sa/def :image/content_type (sa/nilable string?))
(sa/def :image/url (sa/nilable string?))
(sa/def :image/thumbnail_url (sa/nilable string?))
(sa/def :image/to_delete (sa/nilable boolean?))
(sa/def ::manufacturer any?)
(sa/def ::version string?)
(sa/def ::created_at any?)
(sa/def ::technical_detail string?)

(sa/def :any/id any?) ;; UUID spec

(sa/def :nil/technical_detail (sa/nilable string?))
(sa/def :nil/version (sa/nilable string?))
(sa/def :nil/description (sa/nilable string?))
(sa/def :nil/rental_price (sa/nilable string?))
(sa/def :nil/cover_image_id (sa/nilable string?))
(sa/def :nil/hand_over_note (sa/nilable string?))
(sa/def :nil/internal_description (sa/nilable string?))
(sa/def :nil/info_url (sa/nilable string?))

(sa/def :license/post-license (sa/keys :req-un [::inventory_code]
                                       :opt-un [::item_id
                                                ::id
                                                ::owner_id
                                                ::p4u
                                                ::total_quantity

                                                ::operating_system
                                                ::quantity_allocations
                                                ::maintenance_currency

                                                ::maintenance_price
                                                ::maintenance_expiration
                                                ::project_number

                                                ::license_expiration
                                                ::reference
                                                ::installation
                                                ::dongle_id
                                                ::procured_by
                                                ::maintenance_contract
                                                ::license_type
                                                ::activation_type

                                                ::is_borrowable
                                                :nil/retired

                                                ::is_inventory_relevant
                                                :nil/last_check

                                                :nil/shelf
                                                :nil/status_note
                                                :nil/name
                                                ::attachments
                                                :nil/invoice_number
                                                ::is_broken

                                                :nil/note
                                                :nil/serial_number

                                                ::updated_at
                                                :nil/retired_reason
                                                :nil/responsible
                                                :any/invoice_date
                                                ::model_id
                                                :nil/supplier_id
                                                :nil/parent_id
                                                ::inventory_pool_id
                                                ::is_incomplete
                                                :nil/item_version
                                                ::needs_permission
                                                :nil/user_name
                                                ::room_id
                                                :any/price
                                                ::created_at
                                                :nil/insurance_number]))

(sa/def :package/payload (sa/keys :req-un [::room_id
                                           ::model_id]
                                  :opt-un [::owner_id
                                           :nil/price
                                           :nil/shelf
                                           :nil/status_note
                                           :nil/note
                                           ::last_check
                                           ::inventory_code
                                           ::retired
                                           ::is_broken
                                           ::is_incomplete
                                           ::is_borrowable
                                           :any/items_attributes
                                           :nil/retired_reason
                                           ::is_inventory_relevant
                                           :nil/user_name]))
