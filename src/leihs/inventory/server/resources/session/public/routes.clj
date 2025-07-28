(ns leihs.inventory.server.resources.session.public.routes
  (:require
   [clojure.set]
   [clojure.test :refer :all]
   [crypto.random]
   [cryptohash-clj.api :refer :all]
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
            :handler session-public/get-resource}}]]])
