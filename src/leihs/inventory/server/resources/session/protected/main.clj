(ns leihs.inventory.server.resources.session.protected.main
  (:require
   [leihs.inventory.server.utils.request :refer [authenticated? get-auth-entity]]
   [taoensso.timbre :refer [debug]]))

(defn get-resource [request]
  (if (authenticated? request)
    (do
      (debug "User authenticated with:" (get-auth-entity request))
      {:status 200
       :body {:message "Access granted to protected resource"
              :token (get-auth-entity request)}})
    (do
      (debug "User not authenticated")
      {:status 401 :body "Not authenticated"})))
