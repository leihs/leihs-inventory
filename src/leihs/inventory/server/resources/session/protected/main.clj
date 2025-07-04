(ns leihs.inventory.server.resources.session.protected.main
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
   ;[leihs.inventory.server.constants :refer [HIDE_BASIC_ENDPOINTS APPLY_DEV_ENDPOINTS]]
   ;[leihs.inventory.server.resources.session.protected.session :as ab]
   ;[leihs.inventory.server.resources.utils.request :refer [AUTHENTICATED_ENTITY authenticated? get-auth-entity]]

   [leihs.inventory.server.resources.session.public.main :refer [public-endpoint-handler]]
   [leihs.inventory.server.resources.utils.request :refer [AUTHENTICATED_ENTITY authenticated? get-auth-entity]]

   [next.jdbc :as jdbc]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.util.response :as response]
   [schema.core :as s])
  (:import (com.google.common.io BaseEncoding)
           (java.time Duration Instant)
           (java.util Base64 UUID)))


(defn protected-handler [request]
  (if (authenticated? request)
    (do
      (println "User authenticated with:" (get-auth-entity request))
      {:status 200
       :body {:message "Access granted to protected resource"
              :token (get-auth-entity request)}})
    (do
      (println "User not authenticated")
      {:status 403 :body "Forbidden"})))
