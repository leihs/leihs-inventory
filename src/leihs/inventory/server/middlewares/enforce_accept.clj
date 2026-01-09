(ns leihs.inventory.server.middlewares.enforce-accept
  (:require
   [clojure.string :as str]
   [leihs.inventory.server.utils.resource-handler :refer [custom-not-found-handler]]))

(defn wrap-enforce-accept
  "Enforces :accept constraints from matched route data.
   Both mismatches fallback to custom-not-found-handler."
  [handler]
  (fn [request]
    (let [method (:request-method request)
          route-data (get-in request [:reitit.core/match :data method])
          route-accept (:accept route-data)
          accept-header (str/lower-case
                         (or (get-in request [:headers "accept"]) "*/*"))
          has-html? (str/includes? accept-header "text/html")
          has-json? (str/includes? accept-header "application/json")
          has-wildcard-only? (and (str/includes? accept-header "*/*")
                                  (not has-html?)
                                  (not has-json?)
                                  (not (str/includes? accept-header "image/")))
          is-html-request? (or has-html? has-wildcard-only?)
          is-json-only-request? (and has-json? (not has-html?) (not has-wildcard-only?))]

      (cond
        ;; JSON route + HTML request → fallback to not-found handler
        (and (= route-accept "application/json")
             is-html-request?
             (not has-json?))
        (custom-not-found-handler request)

        ;; HTML route + JSON-only request → fallback to not-found handler
        (and (= route-accept "text/html")
             is-json-only-request?)
        (custom-not-found-handler request)

        ;; Otherwise continue normally
        :else
        (handler request)))))
