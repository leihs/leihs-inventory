(ns leihs.inventory.server.resources.admin.status.routes
  (:require
   [clojure.set]
   [leihs.core.status :as status]
   [leihs.inventory.server.resources.utils.middleware :refer [wrap-is-admin!]]
   [leihs.inventory.server.constants :refer [HIDE_BASIC_ENDPOINTS ]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn get-admin-status-routes []
  ["/admin/status/"
   { :no-doc HIDE_BASIC_ENDPOINTS
    :get {:accept "application/json"
          :handler status/status-handler
          :middleware [wrap-is-admin!]}}])
