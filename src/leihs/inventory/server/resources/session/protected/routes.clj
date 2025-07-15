(ns leihs.inventory.server.resources.session.protected.routes
  (:require
   ;[buddy.auth.backends.token :refer [jws-backend]]
   ;[buddy.auth.middleware :refer [wrap-authentication]]
   ;[buddy.sign.jwt :as jwt]
   ;[cider-ci.open-session.bcrypt :refer [checkpw hashpw]]
   ;[clojure.set]
   ;[clojure.string :as str]
   ;[clojure.test :refer :all]
   ;[clojure.tools.logging :as log]
   ;[crypto.random]
   ;[cryptohash-clj.api :refer :all]
   ;[digest :as d]
   ;[honey.sql :refer [format] :rename {format sql-format}]
   ;[honey.sql.helpers :as sql]
   [leihs.inventory.server.constants :refer [HIDE_BASIC_ENDPOINTS ]]
   [leihs.inventory.server.resources.session.protected.main :refer [get-resource]]
   [leihs.inventory.server.utils.auth.session :as ab]
   ;[ring.util.response :as response]
   ;[schema.core :as s]
   )
  (:import (com.google.common.io BaseEncoding)
           (java.time Duration Instant)
           (java.util Base64 UUID)))

(defn routes []
  ["/"
   {:no-doc HIDE_BASIC_ENDPOINTS}

   ["session"
    {:tags ["Auth / Session"]}

    ["/protected"
     {:get {:accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :swagger {:security [{:csrfToken []}]}
            :handler get-resource
            :middleware [ab/wrap-session-authorize!]}}]]])
