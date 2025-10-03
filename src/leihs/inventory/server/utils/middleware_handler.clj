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
    (if (and accept (str/includes? accept "application/json")) ;; FIXME
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
  (let [whitelist #{"/inventory/" "/inventory"} ;; ✅ inline whitelist
        match-ok? (fn [u]
                    (when-let [match (r/match-by-path router u)]
                      (let [route-data (get-in match [:data method])
                            fallback? (get-in match [:data :fallback?])]
                        (when (and route-data (not fallback?))
                          route-data))))]
    (or (match-ok? uri)
        (match-ok? (if (.endsWith uri "/")
                     (subs uri 0 (dec (count uri))) ;; drop slash
                     (str uri "/")))
        (when (contains? whitelist uri)
          true))))

(defn pr
  ([str fnc]
  ;(println ">oo> HELPER / " str fnc)(println ">oo> HELPER / " str fnc)
  (println ">oo> " str fnc)
  fnc
  )

  ([str str2 fnc]
  ;(println ">oo> HELPER / " str fnc)(println ">oo> HELPER / " str fnc)
  (println ">oo> " str str2)
  fnc
  )
)

(defn wrap-strict-format-negotiate
  "- If the endpoint produces a content type that is accepted by the client,
     the request is passed to the handler.
   - If the endpoint does not produce a content type that is accepted by the client,
     a 404 response is returned.
   - If the endpoint does not exist, a 404 response is returned.
   - If the Accept header is application/json, the request is always passed to the handler
     (to allow error responses in JSON format).
   - For routes matching /inventory(/.*)? and resulting in a 404, an HTML response is returned
     if text/html or */* is accepted by the client.
   - For other routes resulting in a 404, a plain text 404 response is returned."
  [handler]
  (fn [request]

    (println ">o> abc.wrap-strict-format-negotiate")

    (let [accept-header (get-in request [:headers "accept"])
          accepted-types (if accept-header (parse-accept-header accept-header) #{"*/*"})
          method (get request :request-method)
          route-data (get-in request [:reitit.core/match :data method])

          is-accept-json? (= accept-header "application/json")

          ;p (println ">o> abc.route-data" route-data)
          produces-set (set (map clojure.string/lower-case (get route-data :produces [])))
          accept-format (some-> route-data :accept clojure.string/lower-case)
          allowed-formats (cond-> produces-set
                            accept-format (conj accept-format))
          uri (:uri request)
          endpoint-produces-content-type? (boolean (and (seq allowed-formats)
                                               (seq accepted-types)
                                               (some allowed-formats accepted-types)))
          is-inventory-route? (re-matches #"/inventory(/.*)?" uri)

          p (println ">o> abc.endpoint-produces-content-type?.200==true" endpoint-produces-content-type?)
          p (println ">o> abc.allowed-formats" allowed-formats)
          p (println ">o> abc.accepted-types" accepted-types)

          router (:reitit.router request)
          method (:request-method request)
          uri (:uri request)

          ;p (println ">o> abc1" router)
          route-data (endpoint-exists? router method uri)

          ;p (println ">o> abc.a" router)
          ;p (println ">o> abc.b" method uri)

          exists? (boolean route-data)

          ;resp-status (if (and endpoint-produces-content-type? exists? (not is-accept-json?)) 200 404)
          ;resp-status 200

          resp-status (cond
                        ;(and endpoint-produces-content-type? exists? (not is-accept-json?)) 200
                        ;(and (not endpoint-produces-content-type?) exists? (not is-accept-json?)) 404
                        (and exists? (not is-accept-json?)) 200
                        (and (not exists?) (not is-accept-json?)) 404

                        (and endpoint-produces-content-type? exists? is-accept-json?) 404

                        :else 404
                        )


          p (println ">o> abc.nego.exists?" exists?)
          p (println ">o> abc.nego.resp-status" resp-status)
          p (println ">o> abc.nego.endpoint-produces-content-type?" endpoint-produces-content-type?)

          ]

      (if endpoint-produces-content-type?
        (pr ">o>" "forward" (handler request))

        (if is-inventory-route?
          (create-accept-response request resp-status)
          (-> (response "") (status resp-status) (content-type "text/html")))
        ;nil
        )

      ;(handler request)
      ;nil
      )))


(defn pr
  ([str fnc]
  ;(println ">oo> HELPER / " str fnc)(println ">oo> HELPER / " str fnc)
  (println ">oo> " str fnc)
  fnc
  )

  ([str str2 fnc]
  ;(println ">oo> HELPER / " str fnc)(println ">oo> HELPER / " str fnc)
  (println ">oo> " str str2)
  fnc
  )
)

(defn wrap-html-40x
  "Wraps a handler so that for matching URIs (by regex) with Accept text/html,
   if the handler returns a 40x, we return SPA-HTML response"
  [handler url-patterns]
  (fn [request]
    (println ">o> abc.wrap-html-40x")
    (let [resp (handler request)
          uri (:uri request)
          accept (some-> (get-in request [:headers "accept"]) str/lower-case)
          resp-status (:status resp)
          p (println ">o> abc.wrap-html-404 -> " (:status resp))]
      (if (and (#{400 404 422} resp-status)
               (some #(re-matches % uri) url-patterns)
               (or (str/includes? accept "text/html")
                   (str/includes? accept "*/*")))
        (pr ">o>" "1" (create-accept-response request 404))
        (pr ">o>" "2" resp)))))
