(ns leihs.inventory.server.app-handler
  (:require
   [leihs.core.anti-csrf.back :as anti-csrf]
   [leihs.core.db :as db]
   [leihs.core.http-cache-buster2 :as cache-buster2]
   [leihs.core.ring-audits :as ring-audits]
   [leihs.core.routing.back :as core-routing]
   [clojure.string :as str]
   [ring.util.response :refer [bad-request response status content-type]]
   ;[ring.util.response :refer [bad-request response status]]
   [cheshire.core :as json]

   [ring.util.mime-type :as mime]
   [ring.middleware.resource :refer [wrap-resource]]
   [ring.middleware.file-info :refer [wrap-file-info]]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults]]

   [leihs.core.routing.dispatch-content-type :as dispatch-content-type]
   [leihs.inventory.server.resources.routes :as routes]
   [leihs.inventory.server.swagger :as swagger]
   [leihs.inventory.server.utils.coercion :refer [wrap-handle-coercion-error]]
   [leihs.inventory.server.utils.csrf-handler :as csrf]
   [leihs.inventory.server.utils.debug-handler :as debug-mw]
   [leihs.inventory.server.utils.middleware :refer [wrap-authenticate!]]
   [leihs.inventory.server.utils.middleware_handler :refer [default-handler-fetch-resource
                                                            wrap-accept-with-image-rewrite
                                                            wrap-session-token-authenticate!]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [leihs.inventory.server.utils.ressource-handler :refer [custom-not-found-handler]]
   [leihs.inventory.server.utils.session-dev-mode :as dm]
   [muuntaja.core :as m]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [reitit.dev.pretty :as pretty]
   [reitit.ring :as ring]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.exception :as exception]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]

   [reitit.swagger :as swagger2]
   [reitit.swagger-ui :as swagger-ui2]

   [reitit.swagger]
[clojure.string :as clojure.string]
[leihs.inventory.server.utils.request-utils :refer [authenticated?]]
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.cookies :refer [wrap-cookies]]
   [ring.middleware.default-charset :refer [wrap-default-charset]]
   [ring.middleware.params :refer [wrap-params]]
   [taoensso.timbre :as timbre :refer [debug spy]]))




(defn parse-cookie [request]
  (let [cookie-str (get-in request [:headers "cookie"])]
    (if (or (nil? cookie-str) (clojure.string/blank? cookie-str))
      {}
      (->> (clojure.string/split cookie-str #"; ")
        (map #(clojure.string/split % #"=" 2))
        (reduce
          (fn [m [k v]]
            (if (and k v)
              (assoc m k v)
              m))
          {})))))

(defn session-valid? [request]
  (let [session (parse-cookie request)
        is-authenticated? (authenticated? request)]
    (and is-authenticated?
      (get session "leihs-user-session"))))

(defn parse-accept-header [accept-header]
  (->> (clojure.string/split accept-header #",")
    (map #(clojure.string/trim (clojure.string/lower-case (clojure.string/replace % #";.*" ""))))
    (remove clojure.string/blank?)
    set))


(defn pr [str fnc]
  ;(println ">oo> HELPER / " str fnc)(println ">oo> HELPER / " str fnc)
  (println ">oo> " str fnc)
  fnc
  )

(defn create-accept-response
  "Return a response based on the Accept header.
   - If Accept contains text/html → return an HTML response
   - If Accept contains application/json → return a JSON response
   - Otherwise → return a plain text response"
  [request  status ]

     (let [accept-header (get-in request [:headers "accept"])

           status (int status)

           p (println ">o> abc" status (type status))
           ]

  (cond
    (and accept-header (str/includes? accept-header "text/html"))
    (-> (rh/index-html-response request status)
      (status status)
      (content-type "text/html"))

    (and accept-header (str/includes? accept-header "application/json"))
    (-> (response {:error (str "Status " status)})
      (status status)
      (content-type "application/json"))

    :else
    (-> (response (str "Error " status))
      (status status)
      (content-type "text/plain"))))
       )



(defn create-accept-response
  "Return a response based on the Accept header.
   - text/html -> HTML response
   - application/json -> JSON response
   - otherwise -> plain text"
  [request http-status]
  (let [accept (get-in request [:headers "accept"])
        code   (int http-status)]
    (cond
      (and accept (str/includes? accept "text/html"))
      (rh/index-html-response request code)

      (and accept (str/includes? accept "application/json")) ;; FIXME
        (-> (response (json/generate-string {:status  "failure"
                                                  :message "Error occurred"}))
        (status code)
        (content-type "application/json"))

      :else
      (-> (response (str "Error " code))
        (status code)
        (content-type "text/plain")))))


(defn wrap-strict-format-negotiate [handler]
  (fn [request]
    (let [accept-header (get-in request [:headers "accept"])
          accepted-types (if accept-header (parse-accept-header accept-header) #{"*/*"})

          ;; method = :get, :post, etc.
          method (get request :request-method)
          route-data (get-in request [:reitit.core/match :data method])

          uri (:uri request)

          ;; get :produces list or single :accept format
          produces-set (set (map clojure.string/lower-case (get route-data :produces [])))
          accept-format (some-> route-data :accept clojure.string/lower-case)
          allowed-formats (cond-> produces-set
                            accept-format (conj accept-format))]

      ;(println "> strict-format:    " (:uri request) method)
      ;(println "> strict-format :: Accept Header:    " accept-header)
      ;(println "> strict-format :: Parsed Formats:   " accepted-types)
      ;(println "> strict-format :: Allowed Formats:  " allowed-formats)

      ;; enforce: if Accept is given, it must match what we allow
      (if (and (seq allowed-formats)
            (seq accepted-types)
            (not (some allowed-formats accepted-types)))

        ;; TODO: return default page

        ;(and accept-html? (not (session-valid? request)) (not swagger-call?))

        ;(cond
        ;  ;  (and (str/includes? accept-header "text/html") (pr "session-invalid?" (not (session-valid? request))))
        ;  ;{:status 302 :headers {"Location" "/sign-in?return-to=%2Finventory" "Content-Type" "text/html"} :body ""}
        ;  ;
        ;
        ;  (str/includes? accept-header "text/html")
        ;  (pr "1?" (rh/index-html-response request 404))
        ;
        ;  ;
        ;  ;(or (= uri "/inventory/api-docs") (= uri "/inventory/api-docs/"))
        ;  ;;{:status 302 :headers {"Location" "/inventory/api-docs/index.html"} :body ""}
        ;  ;(handler request)
        ;  ;
        ;  ;(or (= uri "/inventory/swagger-ui") (= uri "/inventory/swagger-ui/"))
        ;  ;;{:status 302 :headers {"Location" "/inventory/swagger-ui/index.html"} :body ""}
        ;  ;(handler request)
        ;
        ;  ;:else (-> (pr "meins>>" {:status 406 })
        ;  ;        response
        ;  ;        (status 406)
        ;  ;        )
        ;
        ;  :else           (pr "2" (rh/index-html-response request 200))
        ;
        ;
        ;  )


        (create-accept-response request  404)


        (handler request)))))





(def middlewares [debug-mw/wrap-debug

                  wrap-strict-format-negotiate


                  wrap-handle-coercion-error
                  db/wrap-tx
                  core-routing/wrap-canonicalize-params-maps
                  muuntaja/format-middleware
                  ring-audits/wrap
                  wrap-accept-with-image-rewrite

                  csrf/extract-header

                  wrap-session-token-authenticate!
                  ;wrap-authenticate!

                  wrap-cookies
                  csrf/wrap-csrf
                  leihs.core.anti-csrf.back/wrap
                  dm/extract-dev-cookie-params


                  wrap-params
                  wrap-content-type
                  dispatch-content-type/wrap-accept
                  ;default-handler-fetch-resource

                  reitit.swagger/swagger-feature
                  parameters/parameters-middleware
                  muuntaja/format-negotiate-middleware
                  muuntaja/format-response-middleware

                  exception/exception-middleware

                  muuntaja/format-request-middleware
                  coercion/coerce-response-middleware
                  coercion/coerce-request-middleware
                  multipart/multipart-middleware])

(def default-router-config {:conflicts nil
                            :strict-slash true
                            :exception pretty/exception
                            :data {:coercion reitit.coercion.spec/coercion
                                   :muuntaja m/instance
                                   :middleware middlewares}})


;(:require [ring.util.mime-type :as mime]))

(def default-mime
  {"svg"  "image/svg+xml"
   "svgz" "image/svg+xml"})

(defn ensure-content-type [handler]
  (fn [req]
    (let [resp (handler req)]
      (if (pr "contType??" (get-in resp [:headers "Content-Type"]))
        resp
        (if-let [ct (pr "ct???" (mime/ext-mime-type (:uri req) default-mime))]
          (assoc-in resp [:headers "Content-Type"] ct)
          resp)))))


(def digest-re
  ;; matches: /path/name.<ext>_<40hex>.<ext>
  ;; captures: 1=/path/name, 2=first ext, 3=second ext
  (re-pattern "(?i)^(.*?)(\\.[^.\\/]+)_[0-9a-f]{40}(\\.[^.\\/]+)$"))


(def digest-tail-re
  ;; matches: _<hex>.<ext> at end of path, e.g. /name.svg_<hash>.svg
  #"(?i)_[0-9a-f]{6,64}\.[^./]+$")

(defn strip-digest [handler]
  (fn [req]
    (let [uri (:uri req)
          new-uri (if (and uri (re-find digest-tail-re uri))
                    (str/replace uri digest-tail-re "")
                    uri)]
      (println ">o> strip-digest" uri "=>"
        new-uri) ; optional debug
      (handler (assoc req :uri new-uri)))))

(def cache-bust-options
  {:cache-bust-paths [#"^/inventory/assets/.*\.(js|css|png|jpg|svg|woff2?)$"]
   :never-expire-paths []
   :cache-enabled? true})

;(defn init []
;  (let [router
;        (ring/router (routes/all-api-endpoints) default-router-config)
;        swagger-ui-handler (swagger/init)
;        default-handler (ring/routes swagger-ui-handler
;                                     (ring/create-default-handler {:not-found custom-not-found-handler}))]
;    (-> (ring/ring-handler router default-handler)
;        (cache-buster2/wrap-resource "public" cache-bust-options)
;        (wrap-content-type {:mime-types {"svg" "image/svg+xml"}})
;        (wrap-default-charset "utf-8"))))

(require '[ring.middleware.defaults :refer [wrap-defaults site-defaults]])

(def defaults
  (-> site-defaults
    (assoc-in [:responses :not-modified-responses] false)))

(defn tap-status [handler]
  (fn [req]
    (let [resp (handler req)]
      (println ">> status" (:status resp) "uri" (:uri req)
        "ct" (get-in resp [:headers "Content-Type"]))
      resp)))


(def tail-re #"(?i)_[0-9a-f]{6,64}\.[^./]+$")

(defn- strip-tail [s]
  (when s
    (let [new (str/replace s tail-re "")]
      (if (identical? s new) s new))))

(defn strip-digest [handler]
  (fn [req]
    (let [uri      (:uri req)
          pinfo    (:path-info req)
          new-uri  (strip-tail uri)
          new-pi   (strip-tail pinfo)
          req' (cond-> req
                 (and new-uri (not= new-uri uri))     (assoc :uri new-uri)
                 (and new-pi  (not= new-pi  pinfo))   (assoc :path-info new-pi))]
      (when (or (not= uri new-uri) (not= pinfo new-pi))
        (println ">o> strip-digest" uri "=>" new-uri
          (when pinfo (str " | path-info " pinfo " => " new-pi))))
      (handler req'))))


(defn ensure-content-type [handler]
  (fn [req]
    (let [resp (handler req)]
      (if (get-in resp [:headers "Content-Type"])
        resp
        (if-let [ct (mime/ext-mime-type (:uri req)
                      {"svg" "image/svg+xml" "svgz" "image/svg+xml"})]
          (assoc-in resp [:headers "Content-Type"] ct)
          resp)))))

(defn init []
  (let [router (ring/router (routes/all-api-endpoints) default-router-config)
        swagger-ui-handler (swagger/init)
        not-found (ring/create-default-handler {:not-found custom-not-found-handler})

        ;; Try Swagger first, then the router, then 404.
        app (ring/routes
              swagger-ui-handler
              (ring/ring-handler router not-found))]
              ;(ring/ring-handler not-found))]

    (->
      app

      (wrap-content-type {:mime-types {"svg"  "image/svg+xml"
                                       "svgz" "image/svg+xml"}})

      strip-digest


      (cache-buster2/wrap-resource "public" cache-bust-options)
      ;ensure-content-type

      (wrap-file-info {:mime-types {"svg"  "image/svg+xml"
                                    "svgz" "image/svg+xml"}})


      ;(wrap-defaults defaults)

      ;(wrap-content-type {:mime-types {"svg" "image/svg+xml"}})
      ;(wrap-default-charset "utf-8")
      ;
      ;(wrap-defaults (-> site-defaults
                       ; don’t let not-found HTML mask static results
                       ; leave as-is unless you’ve changed defaults
                       ;identity))
      ;(wrap-defaults defaults)
      ;tap-status

      ensure-content-type


      )))
