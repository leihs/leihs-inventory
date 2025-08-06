(ns leihs.inventory.server.resources.pool.options.types
  (:require
   [clojure.spec.alpha :as sa]
   [leihs.inventory.server.resources.pool.models.basic_coercion :as sp]
   [reitit.coercion.schema]))

(def response-option-object {:id uuid?
                             :inventory_code string?
                             :product string?
                             :name string?
                             :version (sa/nilable string?)
                             :price (sa/nilable :nil-pos-number/price)})
(def response-option-post response-option-object)

(sa/def ::data (sa/coll-of ::response-option-object))
(sa/def ::pagination any?)

(sa/def ::response-options-container
  (sa/keys :req-un [::data ::pagination]))

(def response-option-get
  (sa/or :multiple (sa/coll-of ::response-option-object)
         :paged ::response-options-container))

(sa/def ::response-option-object
  (sa/keys :req-un [::sp/id
                    ::sp/inventory_code
                    ::sp/name
                    ::sp/product]
           :opt-un [:nil/version :nil-pos-number/price]))

(sa/def ::data (sa/coll-of ::response-option-object))
(sa/def ::pagination any?)

(sa/def :option/body (sa/keys :req-un [::sp/product
                                       ::sp/inventory_code]
                              :opt-un [:nil/version
                                       :nil-pos-number/price]))

(sa/def ::options-query
  (sa/keys :opt-un [::sp/page ::sp/size]))
