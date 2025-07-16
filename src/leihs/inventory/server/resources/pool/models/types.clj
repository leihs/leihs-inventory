(ns leihs.inventory.server.resources.pool.models.types
  (:require
   [clojure.spec.alpha :as sa]
   [leihs.inventory.server.resources.pool.models.basic_coercion :as sp]
   [leihs.inventory.server.resources.types :refer [pagination]]
   [reitit.coercion.schema]
   [schema.core :as s]
   [spec-tools.core :as st]))

(def description-model-form "CAUTION:\n
- Model\n
   - Modifies all attributes except: Images/Attachments\n
   - Use PATCH /inventory/<pool-id>/model/<image-id> to set is_cover\n
   - GET: contains all data for fields (attachment, image included)\n
- Full sync will be processed for: accessories, compatibles, categories, entitlements, properties\n
- Image\n
   - Use POST /inventory/models/<model-id>/images to upload image\n
   - Use DELETE /inventory/models/<model-id>/images/<image-id> to delete image\n
- Attachment\n
   - Use POST /inventory/models/<model-id>/attachments to upload attachment\n
   - Use DELETE /inventory/models/<model-id>/attachments/<attachment-id> to delete attachment")

(sa/def ::image_attribute (sa/keys :opt-un [:image/filename
                                            :upload/content_type
                                            :image/url
                                            :image/to_delete
                                            :image/thumbnail_url] :req-un [:image/id :image/is_cover]))

(sa/def :model2/image_attributes
  (sa/or :multiple (sa/or :coll (sa/coll-of ::image_attribute)
                          :str string?)
         :none empty?))

(sa/def :create-model/scheme
  (sa/keys
   :req-un [::sp/is_package
            ::sp/product
            ::sp/id]
   :opt-un [::sp/properties
            :nil/description
            :nil/hand_over_note
            :nil/manufacturer
            :nil/version
            :nil/technical_detail
            :nil/internal_description
            ::sp/accessories
            ::sp/entitlements
            ::sp/attachments
            :nil/cover_image_id
            ::sp/categories
            :model2/image_attributes
            ::sp/compatibles]))

(sa/def :model-optional-response/inventory-model
  (st/spec {:spec :create-model/scheme
            :description "Complete inventory response"}))

(def post-response :model-optional-response/inventory-model)

(def get-model-scheme
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
   (s/optional-key :content_type) (s/maybe s/Str)
   (s/optional-key :image_url) (s/maybe s/Str)})

(def get-models-response-payload
  (merge get-model-scheme {s/Keyword s/Any}))

(def get-response {:data [get-models-response-payload] :pagination pagination})

(sa/def :software/properties (sa/or
                              :single (sa/or :coll (sa/coll-of ::sp/property)
                                             :str string?)
                              :none nil?))

(sa/def :model/multipart
  (sa/keys
   :req-un [::sp/product]
   :opt-un [::sp/version
            ::sp/manufacturer
             ;:nil-str/is_package
            :nil/is_package
            :nil/description
            ::sp/technical_detail
            :nil/internal_description
            :nil/hand_over_note
            ::sp/categories
            ::sp/owner
            :min/compatibles
            ::sp/entitlements
            :software/properties
            ::sp/accessories]))
