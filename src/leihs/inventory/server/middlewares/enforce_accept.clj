(ns leihs.inventory.server.middlewares.enforce-accept
  (:require
   [clojure.string :as str]
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
          accept-header (str/lower-case
                         (or (get-in request [:headers "accept"]) "*/*"))
          has-html? (str/includes? accept-header "text/html")
          has-json? (str/includes? accept-header "application/json")
          has-image? (str/includes? accept-header "image/")
          has-wildcard-only? (and (str/includes? accept-header "*/*")
                                  (not has-html?)
                                  (not has-json?)
                                  (not has-image?))
          is-html-request? (or has-html? has-wildcard-only?)
          is-json-only-request? (and has-json? (not has-html?) (not has-wildcard-only?))
          route-accepts-images? (and route-produces
                                     (some #(str/includes? % "image/") route-produces))]

      (cond
        ;; Image request to route that doesn't support images
        ;; If not authenticated and route requires auth, pass through to authorize middleware for 401
        ;; Otherwise return 406
        (and has-image?
             (:reitit.core/match request) ; Route exists
             route-produces ; Route has explicit produces
             (not route-accepts-images?))
        (if (and (not route-public?) (not authenticated?))
          (handler request) ; Pass through - route-level authorize middleware will return 401
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
