(ns leihs.inventory.server.app-handler
  (:require
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
  [leihs.inventory.server.utils.middleware-handler :refer [wrap-session-token-authenticate!
                                                            wrap-html-404
                                                            wrap-strict-format-negotiate]]

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

   [ring.util.response :as resp :refer [response content-type status]]


   [reitit.ring :as ring]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [muuntaja.core :as m]

   [ring.middleware.params :refer [wrap-params]]))

(defn strict-produces-middleware [handler]
  (fn [request]
    (let [accept    (get-in request [:headers "accept"])
          ;; get the HTTP method (:get, :post, etc.)
          method    (:request-method request)
          ;; pull produces from method-level data
          produces  (set (get-in request [:reitit.core/match :data method :produces]))
     _ (println ">o> accept" accept "produces" produces)
          ]
      (if (and accept (seq produces)
            (not-any? #(clojure.string/includes? accept %) produces))
        ;; pretend no route matched
        ;nil

        (-> (response "")
          (status 404)
          (content-type "text/html; charset=utf-8"))

        (handler request)))))

(def middlewares [debug-mw/wrap-debug

                  #(wrap-html-404 % [#"/inventory/.+/images/.+"
                                     #"/inventory/.+/images/.+/thumbnail"
                                     #"/inventory/.+/attachments/.+"])


                  ;wrap-strict-format-negotiate
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

                  strict-produces-middleware

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
        (wrap-content-type {:mime-types {"svg" "image/svg+xml"}})
        (wrap-default-charset "utf-8"))))
