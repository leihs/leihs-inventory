(ns leihs.inventory.server.middlewares.enforce-accept
  (:require
   [clojure.string :as str]
   [leihs.inventory.server.utils.resource-handler :refer [custom-not-found-handler]]))

(defn wrap-enforce-accept
  "Enforces :accept constraints from matched route data.
   If route requires JSON but request has HTML Accept → fallback to SPA."
  [handler]
  (fn [request]
    (let [method (:request-method request)
          route-data (get-in request [:reitit.core/match :data method])
          route-accept (:accept route-data)
          accept-header (str/lower-case
                         (or (get-in request [:headers "accept"]) "*/*"))
          has-html? (str/includes? accept-header "text/html")
          has-wildcard-only? (and (str/includes? accept-header "*/*")
                                  (not has-html?)
                                  (not (str/includes? accept-header "image/")))
          is-html-request? (or has-html? has-wildcard-only?)]

      (if (and (= route-accept "application/json")
               is-html-request?)
        ;; Route requires JSON, request wants HTML → fallback to not-found handler
        (custom-not-found-handler request)
        ;; Otherwise continue normally
        (handler request)))))
