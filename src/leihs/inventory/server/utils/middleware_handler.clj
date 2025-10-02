(ns leihs.inventory.server.utils.middleware-handler
  (:require
   [cheshire.core :as json]
   [clojure.string :as str]
   [leihs.core.auth.session :as session]
   [leihs.core.auth.token :as token]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.response-helper :as rh]
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

          ;p (println ">o> abc.route-data" route-data)
          produces-set (set (map clojure.string/lower-case (get route-data :produces [])))
          accept-format (some-> route-data :accept clojure.string/lower-case)
          allowed-formats (cond-> produces-set
                            accept-format (conj accept-format))
          uri (:uri request)
          endpoint-produces-content-type? (and (seq allowed-formats)
                                            (seq accepted-types)
                                            (some allowed-formats accepted-types))
          is-inventory-route? (re-matches #"/inventory(/.*)?" uri)

          ;p (println ">o> abc.endpoint-produces-content-type?.200==true" endpoint-produces-content-type?)
          ;p (println ">o> abc.allowed-formats" allowed-formats)
          ;p (println ">o> abc.accepted-types" accepted-types)
          ]
      (if         endpoint-produces-content-type?
        (handler request)

        (if is-inventory-route?
                                          (create-accept-response request 200)
                                          (-> (response "") (status 200) (content-type "text/html")))
        ;nil
        )

      ;(handler request)
      ;nil
      )))

(defn wrap-html-40x
  "Wraps a handler so that for matching URIs (by regex) with Accept text/html,
   if the handler returns a 40x, we return SPA-HTML response"
  [handler url-patterns]
  (fn [request]
    (let [resp (handler request)
          uri (:uri request)
          accept (some-> (get-in request [:headers "accept"]) str/lower-case)

          p (println ">o> abc.wrap-html-404" (:status resp))
          ]
      (if (and (<= 400 (:status resp))
            (some #(re-matches % uri) url-patterns)
            (or (str/includes? accept "text/html")
              (str/includes? accept "*/*")))
        (create-accept-response request 404)
        resp))))
