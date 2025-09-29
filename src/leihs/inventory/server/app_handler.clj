(ns leihs.inventory.server.app-handler
  (:require
   [cheshire.core :as json]
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
   [leihs.inventory.server.utils.middleware-handler :refer [wrap-session-token-authenticate!]]
   [leihs.inventory.server.utils.response-helper :as rh]
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
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.cookies :refer [wrap-cookies]]
   [ring.middleware.default-charset :refer [wrap-default-charset]]
   ;[ring.middleware.file-info :refer [wrap-file-info]]
   [ring.middleware.params :refer [wrap-params]]
   ;[ring.util.mime-type :as mime]
   [ring.util.response :refer [content-type response status]]))

(defn parse-accept-header [accept-header]
  (->> (clojure.string/split accept-header #",")
       (map #(clojure.string/trim (clojure.string/lower-case (clojure.string/replace % #";.*" ""))))
       (remove clojure.string/blank?)
       set))

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

      :else (-> (response "")
                (status code)
                (content-type "text/html")))))

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

(def middlewares [debug-mw/wrap-debug
                  wrap-strict-format-negotiate
                  wrap-handle-coercion-error
                  db/wrap-tx
                  core-routing/wrap-canonicalize-params-maps
                  muuntaja/format-middleware
                  ring-audits/wrap

                  csrf/extract-header

                  wrap-session-token-authenticate!

                  wrap-cookies
                  csrf/wrap-csrf
                  leihs.core.anti-csrf.back/wrap
                  dm/extract-dev-cookie-params

                  wrap-params
                  wrap-content-type
                  dispatch-content-type/wrap-accept

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

(defn init []
  (let [app (ring/routes
             (swagger/init)
             (ring/ring-handler (ring/router (routes/all-api-endpoints) default-router-config)
                                (ring/create-default-handler {:not-found custom-not-found-handler})))]
    (-> app
        (cache-buster2/wrap-resource "public" cache-bust-options)
     ;(wrap-file-info {:mime-types {"svg" "image/svg+xml"
     ;                              "svgz" "image/svg+xml"}}))))
        (wrap-content-type {:mime-types {"svg" "image/svg+xml"}})
        (wrap-default-charset "utf-8"))))