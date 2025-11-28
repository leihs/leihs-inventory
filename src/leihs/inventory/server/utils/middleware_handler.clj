(ns leihs.inventory.server.utils.middleware-handler
  (:require
   [cheshire.core :as json]
   [clojure.string :as str]
   [leihs.core.auth.session :as session]
   [leihs.core.auth.token :as token]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.response-helper :as rh]
   [reitit.core :as r]
   [ring.middleware.accept]
   [ring.util.response :refer [content-type response status]]))

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

(defn- create-accept-response [request http-status]
  (let [accept (get-in request [:headers "accept"])
        code (int http-status)]
    (if (and accept (str/includes? accept "application/json"))
      (-> (response (json/generate-string {:status "failure"
                                           :message "Error occurred"}))
          (status code)
          (content-type "application/json"))
      (rh/index-html-response request code))))

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

(defn wrap-strict-format-negotiate [handler]
  (fn [request]
    (let [accept-header (get-in request [:headers "accept"])
          accepted-types (if accept-header (parse-accept-header accept-header) #{"*/*"})
          method (:request-method request)
          uri (:uri request)
          router (:reitit.router request)
          route-data (endpoint-exists? router method uri)
          produces-set (set (map clojure.string/lower-case
                                 (get-in request [:reitit.core/match :data method :produces] [])))
          accept-format (some-> route-data :accept clojure.string/lower-case)
          allowed-formats (cond-> produces-set
                            accept-format (conj accept-format))
          is-accept-json? (= accept-header "application/json")
          endpoint-produces? (and (seq allowed-formats)
                                  (seq accepted-types)
                                  (some allowed-formats accepted-types))
          exists? (boolean route-data)
          is-inventory? (re-matches #"/inventory(/.*)?" uri)
          is-html-request? (and accept-header
                                (or (str/includes? accept-header "text/html")
                                    (str/includes? accept-header "*/*")))
          resp-status (cond
                        (and exists? (not is-accept-json?)) 200
                        (and (not exists?) (not is-accept-json?) is-html-request?) 200
                        (and (not exists?) (not is-accept-json?)) 404
                        (and endpoint-produces? exists? is-accept-json?) 404
                        :else 404)]
      (cond
        endpoint-produces? (handler request)
        is-inventory? (create-accept-response request resp-status)

        :else
        (-> (response "") (status resp-status) (content-type "text/html"))))))

(defn wrap-html-40x
  "Wraps a handler so that for matching URIs (by regex) with Accept text/html,
   if the handler returns a 40x, we return SPA-HTML response"
  [handler url-patterns]
  (fn [request]
    (let [resp (handler request)
          uri (:uri request)
          accept (some-> (get-in request [:headers "accept"]) str/lower-case)
          resp-status (:status resp)]
      (if (and (#{400 404 405 422} resp-status)
               (some #(re-matches % uri) url-patterns)
               (or (str/includes? accept "text/html")
                   (str/includes? accept "*/*")))
        (create-accept-response request 404)
        resp))))
