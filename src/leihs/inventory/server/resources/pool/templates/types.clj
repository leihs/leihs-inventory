(ns leihs.inventory.server.resources.pool.templates.types
  (:require
   [clojure.spec.alpha :as sa]
   [clojure.string :as str]
   [leihs.inventory.server.resources.pool.models.basic_coercion :as sp]
   [reitit.coercion.schema]
   [spec-tools.core :as st]))

(sa/def :post/multipart (sa/keys :req-un [::sp/name
                                          ::sp/model_id
                                          ::sp/quantity]))

(sa/def :templates-get/query (sa/keys :opt-un [::sp/page ::sp/size]))

(sa/def ::model
  (sa/keys :req-un [::sp/id ::sp/product ::sp/quantity ::sp/available ::sp/is_quantity_ok]))

(sa/def ::models
  (sa/coll-of ::model :kind vector?))

(sa/def ::post-response
  (sa/keys :req-un [::sp/name ::sp/id ::models]))

(sa/def ::data-keys
  (sa/keys :req-un [::sp/id
                    ::sp/name]
           :opt-un [::sp/is_quantity_ok]))

(sa/def ::data (sa/coll-of ::data-keys))

(sa/def ::get-response
  (sa/or :with-pagination (sa/keys :req-un [::sp/pagination ::data])
         :without-pagination (sa/coll-of ::data-keys)))

(sa/def ::name sp/non-blank-string?)
(sa/def ::quantity sp/non-neg-number?)
(sa/def ::model (sa/keys :req-un [::sp/id ::quantity]))
(sa/def ::models (sa/coll-of ::model :kind vector?))
(sa/def :template/post-put-query (sa/keys :req-un [::name ::models]))
