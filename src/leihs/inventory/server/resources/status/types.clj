(ns leihs.inventory.server.resources.status.types
  (:require
   [schema.core :as s]))

(def db-pool-schema
  {:gauges s/Any
   :timers s/Any})

(def system-status-schema
  {:memory s/Any
   :db-pool db-pool-schema
   :health-checks s/Any})
