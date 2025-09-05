(ns leihs.inventory.server.utils.middleware-handler
  (:require
   [clojure.string :as str]
   [leihs.core.auth.session :as session]
   [leihs.core.auth.token :as token]
   [leihs.core.routing.dispatch-content-type :as dispatch-content-type]
   [leihs.inventory.server.utils.core :refer [valid-attachment-uri?
                                              valid-image-or-thumbnail-uri?]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.ressource-handler :refer [custom-not-found-handler]]
   [ring.middleware.accept]))

(defn default-handler-fetch-resource [handler]
  (fn [request]
    (let [accept-header (get-in request [:headers "accept"])
          uri (:uri request)
          whitelist-uris-for-api ["/sign-in" "/sign-out" "/inventory/api-docs/swagger.json" "/inventory/status"]
          image-or-thumbnail-request? (valid-image-or-thumbnail-uri? uri)
          attachment-request? (valid-attachment-uri? uri)]

      (if (or (and accept-header (some #(str/includes? accept-header %) ["openxmlformats" "text/csv" "json" "image/"]))
              (some #(= % uri) whitelist-uris-for-api)
              image-or-thumbnail-request?
              attachment-request?)
        (handler request)
        (custom-not-found-handler request)))))

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
                      (exception-handler "Error in session-authenticate!" e)
                      handler))
          token (get-in request [:headers "authorization"])
          handler (if (and token
                           (re-matches #"(?i)^token\s+(.*)$" token))
                    (try
                      (token/wrap-authenticate handler)
                      (catch Exception e
                        (exception-handler "Error in token-authenticate!" e)
                        handler))
                    handler)]
      (handler request))))
