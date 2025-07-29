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
                             :technical_detail (sa/nilable string?)
                             :version (sa/nilable string?)
                             :price (sa/nilable any?)})

(def response-option-get [response-option-object])
(def response-option-post response-option-object)

(sa/def :software-post/multipart (sa/keys :req-un [::sp/product]
                                          :opt-un [:nil/version
                                                   :nil/manufacturer
                                                   :nil/description
                                                   ::sp/technical_detail]))

(sa/def ::post-response
  (sa/keys :req-un [:models/type
                    ::sp/product
                    ::sp/id
                    :nil/manufacturer
                    :nil/version
                    ::sp/attachments]

           :opt-un [:nil/description
                    :nil/technical_detail
                    ::sp/updated_at
                    ::sp/created_at]))
