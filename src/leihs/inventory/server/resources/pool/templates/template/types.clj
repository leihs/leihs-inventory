(ns leihs.inventory.server.resources.pool.templates.template.types
  (:require
   [clojure.spec.alpha :as sa]
   [leihs.inventory.server.resources.pool.models.basic-coercion :as sp]
   [reitit.coercion.schema]))

(sa/def ::deleted_ipmg
  (sa/coll-of (sa/keys :req-un [::sp/model_group_id ::sp/inventory_pool_id])
              :kind vector?))

(sa/def ::deleted_template
  (sa/coll-of (sa/keys :req-un [::sp/id ::sp/name ::sp/created_at ::sp/updated_at])
              :kind vector?))

(sa/def ::delete-response
  (sa/keys :req-un [::deleted_ipmg ::deleted_template]))

(sa/def :get/model
  (sa/keys :req-un [::sp/id
                    ::sp/product
                    ::sp/name
                    :nil/version
                    ::sp/quantity
                    ::sp/borrowable_quantity
                    ::sp/is_quantity_ok]
           :opt-un [:nil/cover_image_id
                    :nil/url
                    :nil/content_type
                    :nil/image_id]))

(sa/def :get/models
  (sa/coll-of :get/model :kind vector?))

(sa/def ::get-put-response
  (sa/keys :req-un [::sp/name ::sp/id :get/models]))

(sa/def :put/model (sa/keys :req-un [::sp/id ::sp/quantity]))
(sa/def :put/models (sa/coll-of :put/model :kind vector?))
(sa/def ::put-query (sa/keys :req-un [:non-blank/name :put/models]))
