  (ns leihs.inventory.server.resources.pool.category-tree.types
    (:require
     [clojure.spec.alpha :as sa]
     [schema.core :as s]
     [spec-tools.core :as st]))

(sa/def ::with-metadata boolean?)

(sa/def ::children (sa/coll-of ::category :kind vector?))
(sa/def ::category (sa/keys :opt-un [::metadata]
                            :req-un [::category_id ::name ::children]))

(sa/def ::category_id string?)
(sa/def ::name string?)
(sa/def ::metadata map?)

(sa/def :root/category
  (st/spec
   {:spec (sa/keys :req-un [::name ::children])
    :description "A category node (recursive tree)"}))

(sa/def ::response-body (sa/coll-of :root/category :kind vector?))