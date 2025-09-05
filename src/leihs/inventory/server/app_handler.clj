(ns leihs.inventory.server.app-handler
  (:require
   ;[ring.util.response :refer [bad-request response status]]
   [cheshire.core :as json]
   [clojure.string :as clojure.string]
   [clojure.string :as str]
   [leihs.core.anti-csrf.back :as anti-csrf]
   [leihs.core.db :as db]
   [leihs.core.http-cache-buster2 :as cache-buster2]
   [leihs.core.ring-audits :as ring-audits]
   [leihs.core.routing.back :as core-routing]

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
   [leihs.inventory.server.utils.request-utils :refer [authenticated?]]
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
   [reitit.swagger]
   [reitit.swagger :as swagger2]
   [reitit.swagger-ui :as swagger-ui2]

   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.cookies :refer [wrap-cookies]]

   [ring.middleware.default-charset :refer [wrap-default-charset]]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
   [ring.middleware.file-info :refer [wrap-file-info]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.resource :refer [wrap-resource]]
   [ring.util.mime-type :as mime]
   [ring.util.response :refer [bad-request response status content-type]]
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



(defn parse-accept-header [accept-header]
  (->> (clojure.string/split accept-header #",")
       (map #(clojure.string/trim (clojure.string/lower-case (clojure.string/replace % #";.*" ""))))
       (remove clojure.string/blank?)
       set))

(defn pr [str fnc]
  ;(println ">oo> HELPER / " str fnc)(println ">oo> HELPER / " str fnc)
  (println ">oo> " str fnc)
  fnc)


(defn create-accept-response
  "Return a response based on the Accept header.
   - text/html -> HTML response
   - application/json -> JSON response
   - otherwise -> plain text"
  [request http-status]
  (let [accept (get-in request [:headers "accept"])
        code (int http-status)]
    (cond
      (and accept (str/includes? accept "text/html"))
      (rh/index-html-response request code)

      (and accept (str/includes? accept "application/json")) ;; FIXME
      (-> (response (json/generate-string {:status "failure"
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

        (create-accept-response request 404)

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

(def cache-bust-options
  {:cache-bust-paths [#"^/inventory/assets/.*\.(js|css|png|jpg|svg|woff2?)$"]
   :never-expire-paths []
   :cache-enabled? true})

(require '[ring.middleware.defaults :refer [wrap-defaults site-defaults]])

(def defaults
  (-> site-defaults
      (assoc-in [:responses :not-modified-responses] false)))

(def tail-re #"(?i)_[0-9a-f]{6,64}\.[^./]+$")

(defn- strip-tail [s]
  (when s
    (let [new (str/replace s tail-re "")]
      (if (identical? s new) s new))))

(defn strip-digest [handler]
  (fn [req]
    (let [uri (:uri req)
          pinfo (:path-info req)
          new-uri (strip-tail uri)
          new-pi (strip-tail pinfo)
          req' (cond-> req
                 (and new-uri (not= new-uri uri)) (assoc :uri new-uri)
                 (and new-pi (not= new-pi pinfo)) (assoc :path-info new-pi))]
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
        app (ring/routes
             swagger-ui-handler
             (ring/ring-handler router not-found))]
    (->
     app

     (wrap-content-type {:mime-types {"svg" "image/svg+xml"
                                      "svgz" "image/svg+xml"}})

     strip-digest

     (cache-buster2/wrap-resource "public" cache-bust-options)

     (wrap-file-info {:mime-types {"svg" "image/svg+xml"
                                   "svgz" "image/svg+xml"}})


     ensure-content-type)))
