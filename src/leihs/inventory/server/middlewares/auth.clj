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

(defn endpoint-exists?
  "Check if an endpoint exists for a given method + uri.
   Returns the route data if it's not a fallback, otherwise nil.
   Also accepts URIs with/without a trailing slash.
   Whitelists certain paths like /inventory/ explicitly."
  [router method uri]
  (let [whitelist #{"/inventory/" "/inventory"}
        match-ok? (fn [u]
                    (when-let [match (r/match-by-path router u)]
                      (let [route-data (get-in match [:data method])
                            fallback? (get-in match [:data :fallback?])]
                        (when (and route-data (not fallback?))
                          route-data))))]
    (or (match-ok? uri)
        (match-ok? (if (.endsWith uri "/")
                     (subs uri 0 (dec (count uri)))
                     (str uri "/")))
        (when (contains? whitelist uri)
          true))))
