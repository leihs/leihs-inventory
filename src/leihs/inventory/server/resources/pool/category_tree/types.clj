  (ns leihs.inventory.server.resources.pool.category-tree.types
    (:require
     [clojure.spec.alpha :as sa]))

(sa/def ::with-metadata boolean?)
(sa/def ::response-body {:name string?
                         :children [any?]})
