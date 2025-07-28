(ns leihs.inventory.server.resources.pool.list.types
  (:require
   [clojure.spec.alpha :as sa]
   [leihs.inventory.server.resources.pool.models.basic_coercion :as sp]
   [leihs.inventory.server.resources.types :refer [pagination]]
   [reitit.coercion.schema]
   [schema.core :as s]))

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

(def get-response (s/->Either [{:data [get-models-response-payload] :pagination pagination} [get-models-response-payload]]))

(sa/def :software/properties (sa/or
                              :single (sa/or :coll (sa/coll-of ::sp/property)
                                             :str string?)
                              :none nil?))

(sa/def :model/multipart
  (sa/keys
   :req-un [::sp/product]
   :opt-un [:nil/version
            ::sp/manufacturer
            ::sp/is_package
            :nil/description
            :nil/technical_detail
            :nil/internal_description
            :nil/hand_over_note
            ::sp/categories
            ::sp/owner
            :min/compatibles
            ::sp/entitlements
            :software/properties
            ::sp/accessories]))
