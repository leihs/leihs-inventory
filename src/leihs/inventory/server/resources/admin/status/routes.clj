(ns leihs.inventory.server.resources.admin.status.routes
  (:require
   [clojure.set]
   [leihs.core.auth.session :refer [wrap-authenticate]]
   [leihs.core.status :as status]
   [leihs.inventory.server.constants :refer [HIDE_BASIC_ENDPOINTS APPLY_DEV_ENDPOINTS]]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.auth.auth-routes :refer [logout-handler set-password-handler
                                                              update-role-handler
                                                              session-token-routes]]
   [leihs.inventory.server.resources.utils.flag :as i]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware wrap-is-admin! wrap-authenticate!]]
   [leihs.inventory.server.utils.auth.inventory-auth :refer [wrap-check-authenticated-admin]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))



(defn get-admin-status-routes []

  [""

   ["/admin/status/"
    {:get {:accept "application/json"
           :handler status/status-handler
           :middleware [wrap-is-admin!]}}]

   ])
