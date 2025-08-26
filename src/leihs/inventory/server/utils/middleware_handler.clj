(ns leihs.inventory.server.utils.middleware-handler
  (:require
   [clojure.string :as str]
   [leihs.core.auth.session :as session]
   [leihs.core.auth.token :as token]
   [leihs.core.routing.dispatch-content-type :as dispatch-content-type]
   [ring.middleware.accept]
   [taoensso.timbre :refer [error]]))

(defn wrap-accept-with-image-rewrite [handler]
  (fn [request]
    (let [accept-header (get-in request [:headers "accept"])
          updated-request (cond
                            (str/includes? accept-header "text/html")
                            (assoc-in request [:headers "accept"] "text/html")

                            :else request)]
      ((dispatch-content-type/wrap-accept handler) updated-request))))

(defn wrap-session-token-authenticate! [handler]
  (fn [request]
    (let [handler (try
                    (session/wrap-authenticate handler)
                    (catch Exception e
                      (error "Error in session-authenticate!" e)
                      handler))
          token (get-in request [:headers "authorization"])
          handler (if (and token
                           (re-matches #"(?i)^token\s+(.*)$" token))
                    (try
                      (token/wrap-authenticate handler)
                      (catch Exception e
                        (error "Error in token-authenticate!" e)
                        handler))
                    handler)]
      (handler request))))
