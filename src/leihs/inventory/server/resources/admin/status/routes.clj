(ns leihs.inventory.server.resources.admin.status.routes
  (:require
   [clojure.set]
   [leihs.core.status :as status]
   [leihs.inventory.server.constants :refer [HIDE_BASIC_ENDPOINTS]]
   [leihs.inventory.server.resources.utils.middleware :refer [wrap-is-admin!]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(def db-pool-schema
  {:gauges s/Any
   :timers s/Any})

(def hikari-health-schema
  {"HikariPool-1.pool.ConnectivityCheck"
   {:healthy? s/Bool
    :message (s/maybe s/Str)
    :error (s/maybe s/Str)}})

(def system-status-schema
  {:memory s/Any
   :db-pool db-pool-schema
   :health-checks s/Any})

(defn get-admin-status-routes []
  ["/admin/status/"
   {:no-doc HIDE_BASIC_ENDPOINTS
    :get {:accept "application/json"
          :handler status/status-handler
          :middleware [wrap-is-admin!]
          :coercion reitit.coercion.schema/coercion
          :responses
          {200 {:description "OK"
                :body system-status-schema}
           500 {:description "Internal Server Error"}}}}])

