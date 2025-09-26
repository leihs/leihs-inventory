(ns leihs.inventory.server.resources.status.routes
  (:require
   [clojure.set]
   [leihs.core.status :as status]
   [leihs.inventory.server.constants :refer [HIDE_BASIC_ENDPOINTS]]
   [leihs.inventory.server.resources.status.types :refer [system-status-schema]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]))

(defn routes []
  ["/status"
   {:no-doc HIDE_BASIC_ENDPOINTS
    :get {:accept "application/json"
          :description "Get system status, called by CI"
          :handler status/status-handler
          :public true
          :coercion reitit.coercion.schema/coercion
          :produces ["application/json"]
          :responses
          {200 {:description "OK"
                :body system-status-schema}
           500 {:description "Internal Server Error"}}}}])
