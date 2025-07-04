(ns leihs.inventory.server.resources.session.protected.routes
  (:require
   [leihs.inventory.server.constants :refer [HIDE_BASIC_ENDPOINTS]]
   [leihs.inventory.server.resources.session.protected.main :as session-protected]
   [leihs.inventory.server.utils.auth.session :as ab]))

(defn routes []
  ["/"
   {:no-doc HIDE_BASIC_ENDPOINTS}

   ["session"
    {:tags ["Auth / Session"]}

    ["/protected"
     {:get {:accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :swagger {:security [{:csrfToken []}]}
            :handler session-protected/get-resource
            :middleware [ab/wrap-session-authorize!]}}]]])
