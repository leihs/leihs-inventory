(ns leihs.inventory.server.resources.admin.status.types
  (:require
   ;[clojure.set]
   ;[leihs.core.status :as status]
   ;[leihs.inventory.server.constants :refer [HIDE_BASIC_ENDPOINTS]]
   ;[leihs.inventory.server.resources.utils.middleware :refer [wrap-is-admin!]]
   ;[reitit.coercion.schema]
   ;[reitit.coercion.spec]
   ;[ring.middleware.accept]
   [schema.core :as s]))

(def db-pool-schema
  {:gauges s/Any
   :timers s/Any})

(def system-status-schema
  {:memory s/Any
   :db-pool db-pool-schema
   :health-checks s/Any})


