(ns leihs.inventory.server.resources.session.public.routes
  (:require
   [clojure.set]
   [crypto.random]
   [leihs.inventory.server.constants :refer [HIDE_BASIC_ENDPOINTS]]
   [leihs.inventory.server.resources.session.public.main :as session-public]
   [reitit.coercion.schema]
   [reitit.coercion.spec]))

(defn routes []
  ["/"

   {:no-doc HIDE_BASIC_ENDPOINTS}

   ["session"
    {:tags ["Auth / Session"]}

    ["/public"
     {:get {:swagger {:security []}
            :accept "application/json"
            :public true
            :produces ["application/json"]
            :handler session-public/get-resource}}]]])
