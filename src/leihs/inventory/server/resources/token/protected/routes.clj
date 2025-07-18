(ns leihs.inventory.server.resources.token.protected.routes
  (:require
   [leihs.inventory.server.constants :refer [HIDE_BASIC_ENDPOINTS]]
   [leihs.inventory.server.resources.token.protected.main :as token-protected]))

(defn routes []
  [""
    {:no-doc HIDE_BASIC_ENDPOINTS
     :tags ["Auth / Token"]}

     ["/token/protected"
      {:get {:description "Use 'Token &lt;token&gt;' as Authorization header."
             :accept "application/json"
             :coercion reitit.coercion.schema/coercion
             :swagger {:security [{:apiAuth []}]}
             :handler token-protected/get-resource
             :middleware [token-protected/wrap-token-authentication]}}]])
