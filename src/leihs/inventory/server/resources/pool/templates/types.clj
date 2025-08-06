(ns leihs.inventory.server.resources.pool.templates.types
  (:require
   [clojure.spec.alpha :as sa]
   [leihs.inventory.server.resources.pool.models.basic_coercion :as sp]
   [reitit.coercion.schema]))

(sa/def ::get-query (sa/keys :opt-un [::sp/page ::sp/size]))

(sa/def ::model
  (sa/keys :req-un [::sp/id ::sp/product ::sp/quantity ::sp/available ::sp/is_quantity_ok]))

(sa/def ::models
  (sa/coll-of ::model :kind vector?))

(sa/def ::post-response
  (sa/keys :req-un [::sp/name ::sp/id ::models]))

(sa/def ::data-keys
  (sa/keys :req-un [::sp/id
                    ::sp/name]
           :opt-un [::sp/is_quantity_ok ::sp/models_count]))

(sa/def ::data (sa/coll-of ::data-keys))

(sa/def ::get-response
  (sa/or :with-pagination (sa/keys :req-un [::sp/pagination ::data])
         :without-pagination (sa/coll-of ::data-keys)))

(sa/def :post/model (sa/keys :req-un [::sp/id ::sp/quantity]))
(sa/def :post/models (sa/coll-of :post/model :kind vector?))
(sa/def ::post-query (sa/keys :req-un [:non-blank/name :post/models]))
