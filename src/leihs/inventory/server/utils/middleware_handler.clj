(ns leihs.inventory.server.utils.middleware-handler
  (:require
   [leihs.core.auth.session :as session]
   [leihs.core.auth.token :as token]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
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


(defn- parse-accept-header [accept-header]
  (->> (clojure.string/split accept-header #",")
    (map #(clojure.string/trim (clojure.string/lower-case (clojure.string/replace % #";.*" ""))))
    (remove clojure.string/blank?)
    set))

;(defn create-accept-response
;  "Return a response based on the Accept header.
;   - text/html -> HTML response
;   - application/json -> JSON response
;   - otherwise -> plain text"
;  [request http-status]
;  (let [accept (get-in request [:headers "accept"])
;        code (int http-status)]
;    (cond
;      (and accept (str/includes? accept "text/html"))
;      (rh/index-html-response request code)
;
;      (and accept (str/includes? accept "application/json")) ;; FIXME
;      (-> (response (json/generate-string {:status "failure"
;                                           :message "Error occurred"}))
;        (status code)
;        (content-type "application/json"))
;
;      :else (-> (response "")
;              (status code)
;              (content-type "text/html")))))

(defn- create-accept-response [request http-status]
  (let [accept (get-in request [:headers "accept"])
        code (int http-status)]
    (if (and accept (str/includes? accept "application/json")) ;; FIXME
      (-> (response (json/generate-string {:status "failure"
                                           :message "Error occurred"}))
        (status code)
        (content-type "application/json"))
      (rh/index-html-response request code))))

(defn wrap-strict-format-negotiate [handler]
  (fn [request]
    (let [accept-header (get-in request [:headers "accept"])
          accepted-types (if accept-header (parse-accept-header accept-header) #{"*/*"})
          method (get request :request-method)
          route-data (get-in request [:reitit.core/match :data method])
          produces-set (set (map clojure.string/lower-case (get route-data :produces [])))
          accept-format (some-> route-data :accept clojure.string/lower-case)
          allowed-formats (cond-> produces-set
                            accept-format (conj accept-format))]
      (if (and (seq allowed-formats)
            (seq accepted-types)
            (not (some allowed-formats accepted-types)))

        (create-accept-response request 404)
        (handler request)))))

(defn wrap-html-404
  "Wraps a handler so that for matching URIs (by regex) with Accept text/html,
   if the handler returns a 404, we return a custom HTML response."
  [handler url-patterns]
  (fn [request]
    (let [resp (handler request)
          uri (:uri request)
          accept (some-> (get-in request [:headers "accept"]) str/lower-case)]
      (if (and (= 404 (:status resp))
            (some #(re-matches % uri) url-patterns)
            (or (str/includes? accept "text/html")
              (str/includes? accept "*/*")))
        (create-accept-response request 404)
        resp))))