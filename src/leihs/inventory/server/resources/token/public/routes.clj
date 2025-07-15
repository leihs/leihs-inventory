(ns leihs.inventory.server.resources.token.public.routes
  (:require
   [leihs.inventory.server.constants :refer [HIDE_BASIC_ENDPOINTS APPLY_DEV_ENDPOINTS]]
   [leihs.inventory.server.resources.token.public.main :refer [get-resource]]  )
  (:import (com.google.common.io BaseEncoding)
           (java.time Duration Instant)
           (java.util Base64 UUID)))

(defn routes []
  [["/"

    {:no-doc HIDE_BASIC_ENDPOINTS}

    ["token"
     {:tags ["Auth / Token"]}

     ["/public"
      {:get {:swagger {:security []}
             :handler get-resource}}]]]])
