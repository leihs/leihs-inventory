(ns leihs.inventory.server.middlewares.enforce-accept
  (:require
   [clojure.string :as str]
   [leihs.inventory.server.utils.accept-parser :as accept-parser]
   [leihs.inventory.server.utils.response :refer [custom-not-found-handler]]))

(defn wrap-enforce-accept
  "Enforces :accept constraints from matched route data.
   Both mismatches fallback to custom-not-found-handler."
  [handler]
  (fn [request]
    (let [method (:request-method request)
          route-data (get-in request [:reitit.core/match :data method])
          route-accept (:accept route-data)
          route-produces (:produces route-data)
          route-public? (:public route-data)
          authenticated? (some? (:authenticated-entity request))

          ;; Parse Accept header properly
          accept-header-str (get-in request [:headers "accept"])
          accept-header-lower (str/lower-case (or accept-header-str "*/*"))
          parsed-accept (accept-parser/parse-accept-header accept-header-str)

          ;; Literal string checks for backward compatibility with old routing logic
          has-html? (str/includes? accept-header-lower "text/html")
          has-json? (str/includes? accept-header-lower "application/json")
          has-wildcard-only? (and (str/includes? accept-header-lower "*/*")
                                  (not has-html?)
                                  (not has-json?)
                                  (not (str/includes? accept-header-lower "image/")))
          is-html-request? (or has-html? has-wildcard-only?)
          is-json-only-request? (and has-json? (not has-html?) (not has-wildcard-only?))

          ;; NEW: Proper image-only detection and route satisfaction check
          is-image-only-request? (accept-parser/is-image-only-request? parsed-accept)
          can-satisfy? (accept-parser/can-satisfy-any? parsed-accept route-produces)]

      (cond
        ;; Image-ONLY request (no wildcards/html/json) to route that can't satisfy → 406
        ;; If not authenticated and route requires auth, pass through to authorize middleware for 401
        ;; Otherwise return 406
        (and is-image-only-request?
             (:reitit.core/match request)
             route-produces
             (not can-satisfy?))
        (if (and (not route-public?) (not authenticated?))
          (handler request)
          {:status 406
           :headers {"content-type" "text/plain"}
           :body "Not Acceptable"})

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
