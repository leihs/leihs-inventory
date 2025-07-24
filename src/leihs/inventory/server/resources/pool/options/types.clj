(ns leihs.inventory.server.resources.pool.options.types
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




(sa/def :option/multipart (sa/keys :req-un [::product
                                            ::inventory_code]
                            :opt-un [::version
                                     ::price]))