(ns leihs.inventory.server.resources.pool.software.types
  (:require
   [clojure.spec.alpha :as sa]
   [leihs.inventory.server.resources.pool.models.basic_coercion :as sp]
   [leihs.inventory.server.resources.types :refer [pagination]]
   [reitit.coercion.schema]
   [schema.core :as s]
   [spec-tools.core :as st]))

(def response-option-object {:id uuid?
                             :inventory_pool_id uuid?
                             :inventory_code string?
                             :manufacturer any?
                             :product string?
                             :version (sa/nilable string?)
                             :price (sa/nilable any?)})

(def response-option-get [response-option-object])
(def response-option-post response-option-object)

(sa/def :option/multipart (sa/keys :req-un [::sp/product
                                            ::sp/inventory_code]
                                   :opt-un [::sp/version
                                            ::sp/price]))


(sa/def ::post-response
  (sa/keys :req-un [:nil/description
                    ;::sp/is_package

                    ;::sp/type
                    :models/type

                    ;:nil/hand_over_note
                    ;:nil/internal_description
                    ::sp/product
                    ::sp/id
                    :nil/manufacturer
                    :nil/version
                    ;:nil/technical_detail
                    ]

    :opt-un [
             ;::sp/attachments
             ;::sp/maintenance_period
             ;:nil/rental_price
             ;:nil/cover_image_id
             ::sp/updated_at
             ;:nil/info_url
             ::sp/created_at
             ]))
