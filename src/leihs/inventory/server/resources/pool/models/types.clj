(ns leihs.inventory.server.resources.pool.models.types
  (:require
   [leihs.inventory.server.resources.pool.models.basic_coercion :as sp]
   [schema.core :as s]
   [clojure.spec.alpha :as sa]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [schema.core :as s]
   [spec-tools.core :as st]
   [spec-tools.data-spec :as ds]
   ))

(sa/def ::image_attribute (sa/keys :opt-un [:image/filename
                                            :image/content_type
                                            :image/url
                                            :image/to_delete
                                            :image/thumbnail_url] :req-un [:image/id :image/is_cover]))

(sa/def :create-model/scheme
  (sa/keys
    :req-un [:nil-str/is_package
             ::sp/product
             ::sp/id]
    :opt-un [::sp/properties
             :nil/description
             :nil/hand_over_note
             ::sp/manufacturer
             ::sp/version
             ::sp/technical_detail
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
   (s/optional-key :image_url) (s/maybe s/Str)})

(def get-models-response-payload
  (merge    get-model-scheme    {s/Keyword s/Any}))

(def get-response {:data [get-models-response-payload] :pagination s/Any})

(sa/def :software/properties (sa/or
                               :single (sa/or :coll (sa/coll-of ::sp/property)
                                         :str string?)
                               :none nil?))

(sa/def :model/multipart
  (sa/keys
    :req-un [::sp/product]
    :opt-un [::sp/version
             ::sp/manufacturer
             :nil-str/is_package
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
