(ns leihs.inventory.server.resources.token.public.routes
  (:require
   [leihs.inventory.server.constants :refer [HIDE_BASIC_ENDPOINTS]]
   [leihs.inventory.server.resources.token.public.main :as token-public]))

(defn routes []
  [""
   {:no-doc HIDE_BASIC_ENDPOINTS
    :tags ["Auth / Token"]}

   ["/token/public"
    {:get {:swagger {:security []}
           :handler token-public/get-resource}}]])
