(ns leihs.inventory.server.middlewares.auth
  (:require
   [leihs.core.auth.session :as session]
   [leihs.core.auth.token :as token]
   [leihs.inventory.server.middlewares.exception-handler :refer [exception-handler]]
   [reitit.core :as r]
   [ring.middleware.accept]))

(defn wrap-session-token-authenticate! [handler]
  (fn [request]
    (let [handler (try
                    (session/wrap-authenticate handler)
                    (catch Exception e
                      (exception-handler request "Error in session-authenticate!" e)
                      handler))
          token (get-in request [:headers "authorization"])
          handler (if (and token
                           (re-matches #"(?i)^token\s+(.*)$" token))
                    (try
                      (token/wrap-authenticate handler)
                      (catch Exception e
                        (exception-handler request "Error in token-authenticate!" e)
                        handler))
                    handler)]
      (handler request))))


