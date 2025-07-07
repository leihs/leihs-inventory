(ns leihs.inventory.server.resources.session.protected.main
  (:require
   [leihs.inventory.server.resources.utils.request :refer [authenticated? get-auth-entity]])
  ;(:import (com.google.common.io BaseEncoding)
  ;         (java.time Duration Instant)
  ;         (java.util Base64 UUID))
  )
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
