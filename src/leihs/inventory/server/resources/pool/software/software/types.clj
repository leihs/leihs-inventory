(ns leihs.inventory.server.resources.pool.software.software.types
  (:require
   [clojure.spec.alpha :as sa]
   [leihs.inventory.server.resources.pool.models.basic_coercion :as sp]
   [reitit.coercion.schema]))

(sa/def :software-put/multipart (sa/keys :req-un [::sp/product]
                                         :opt-un [:nil/version
                                                  :nil/manufacturer
                                                  :nil/technical_detail]))

(sa/def ::put-response
  (sa/keys :req-un [:models/type
                    ::sp/product
                    ::sp/id
                    :nil/manufacturer
                    :nil/version]
           :opt-un [:nil/technical_detail
                    ::sp/attachments]))

(sa/def ::image_attribute (sa/keys :req-opt [:image/filename
                                             :image/content_type
                                             :image/url
                                             :image/to_delete
                                             :image/thumbnail_url] :req-un [:image/id :image/is_cover]))

(sa/def :model/image_attributes (sa/or
                                 :single (sa/or :coll (sa/coll-of ::image_attribute)
                                                :str string?)
                                 :none nil?))

(sa/def :software/response
  (sa/keys :req-un [:nil/description
                    ::sp/is_package
                    ::sp/type
                    :nil/hand_over_note
                    :nil/internal_description
                    ::sp/product
                    ::sp/id
                    ::sp/manufacturer
                    :nil/version
                    :nil/technical_detail]

           :opt-un [::sp/attachments
                    ::sp/maintenance_period
                    :nil/rental_price
                    :nil/cover_image_id
                    ::sp/updated_at
                    :nil/info_url
                    ::sp/created_at]))

(def delete-response {:deleted_attachments [{:id uuid?
                                             :model_id uuid?
                                             :filename string?
                                             :size number?}]
                      :deleted_model [{:id uuid?
                                       :product string?
                                       :manufacturer any?}]})
