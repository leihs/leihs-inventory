(ns leihs.inventory.server.resources.pool.responsible-inventory-pools.types
  (:require
   ;[clojure.set]
   ;[leihs.core.auth.session :refer [wrap-authenticate]]
   ;[leihs.inventory.server.constants :refer [fe]]
   ;[leihs.inventory.server.resources.pool.responsible-inventory-pools.main :refer [get-resources]]
   ;[leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware wrap-is-admin!]]
   ;[leihs.inventory.server.utils.response_helper :as rh]
   ;[reitit.coercion.schema]
   ;[reitit.coercion.spec]
   ;[ring.middleware.accept]
   [schema.core :as s]))

(def get-response
   [{:id s/Uuid
    :name s/Str
    :shortname s/Str}])
