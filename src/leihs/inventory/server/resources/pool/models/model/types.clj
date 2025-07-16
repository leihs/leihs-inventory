(ns leihs.inventory.server.resources.pool.models.model.types
  (:require
   [clojure.spec.alpha :as sa]
   [leihs.inventory.server.resources.pool.models.basic_coercion :as sp]
   [reitit.coercion.schema]
   [schema.core :as s]
   [spec-tools.core :as st]))

(s/defschema patch-response {:id s/Uuid
                             :cover_image_id s/Uuid})

(def put-response :model-optional-response/inventory-model)

(def delete-response

  (sa/keys :req-un
           [::deleted_attachments ::deleted_images ::deleted_model]))

(sa/def ::deleted_attachments
  (sa/coll-of
   (sa/keys :req-un [::sp/id ::sp/model_id ::sp/filename ::sp/size])))

(sa/def ::deleted_images (sa/coll-of any?))

(sa/def ::deleted_model
  (sa/coll-of
   (sa/keys :req-un [::sp/id ::sp/product]
            :opt-un [::sp/manufacturer])))

(sa/def :model-get-put-response/inventory-model
  (st/spec {:spec (sa/keys :req-un [::sp/properties
                                    ::sp/is_package
                                    ::sp/accessories
                                    ::sp/entitlements
                                    ::sp/attachments
                                    :model/type
                                    ::sp/categories
                                    ::sp/id
                                    ::sp/compatibles]
                           :opt-un [:min/images
                                    :nil/hand_over_note
                                    :nil/internal_description
                                    ::sp/product])

            :description "Complete inventory response"}))
