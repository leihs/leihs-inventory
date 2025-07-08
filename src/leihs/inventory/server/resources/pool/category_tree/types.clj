  (ns leihs.inventory.server.resources.pool.category-tree.types
    (:require
     [clojure.spec.alpha :as sa]
     ;[leihs.core.core :refer [presence]]
     ;[leihs.core.resources.categories.filter :as filter]
     ;[leihs.core.resources.categories.tree :refer [tree]]
     ;[leihs.inventory.server.utils.auth.roles :as roles]
     ;[reitit.coercion.schema]
     ;[reitit.coercion.spec :as spec]
     ;[reitit.ring.middleware.multipart :as multipart]
     ;[ring.middleware.accept]
     ;[ring.util.response :as response]
     ;[schema.core :as s]
     ))

(sa/def ::with-metadata boolean?)
(sa/def ::response-body {:name string?
                         :children [any?]})
