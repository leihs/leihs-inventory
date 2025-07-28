(ns leihs.inventory.server.resources.pool.options.types
  (:require
   [clojure.spec.alpha :as sa]
   [leihs.inventory.server.resources.pool.models.basic_coercion :as sp]
   [leihs.inventory.server.resources.types :refer [pagination]]
   [reitit.coercion.schema]
   [schema.core :as s]
   [spec-tools.core :as st]))



;(def response-option-get [response-option-object])
;(def response-option-get (sa/or [response-option-object] any?))

;(sa/def ::response-option-get
;(def response-options-get
;  (sa/or :object response-option-object
;    :object {:data [response-option-object]
;             :pagination any?}
;    ;:any    any?
;    ))

(def response-option-object {:id uuid?
                             :inventory_pool_id uuid?
                             :inventory_code string?
                             :manufacturer any?
                             :product string?
                             :version (sa/nilable string?)
                             :price (sa/nilable any?)})
;
;(sa/def ::data (sa/coll-of response-option-object))
;(sa/def ::pagination any?)
;
;(sa/def ::response-options-container
;  (sa/keys :req-un [::data ::pagination]))
;
;(def response-options-get
;  (sa/or :multiple (sa/coll-of response-option-object)
;    :paged  ::response-options-container))





















(sa/def ::response-option-object
  (sa/keys :req-un [::sp/id
                    ::sp/inventory_pool_id
                    ::sp/inventory_code
                    :nil/manufacturer
                    ;:nil/product]
                    ::sp/product]
    :opt-un [:nil/version :nil-any/price]))

;; Define container structure
(sa/def ::data (sa/coll-of ::response-option-object))
(sa/def ::pagination any?)

(sa/def ::response-options-container
  (sa/keys :req-un [::data ::pagination]))

;; Final top-level variant spec
(sa/def ::response-options-get
  (sa/or :multiple (sa/coll-of ::response-option-object)
    :paged    ::response-options-container))


















(def response-option-post response-option-object)



(sa/def :option/multipart (sa/keys :req-un [::sp/product
                                            ::sp/inventory_code]
                                   :opt-un [::sp/version
                                            ::sp/price]))

(sa/def ::options-query
  (sa/keys :opt-un [ ::sp/page ::sp/size]))