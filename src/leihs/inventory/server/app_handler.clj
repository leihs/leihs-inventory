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
   [leihs.inventory.server.utils.middleware :refer [wrap-authenticate!]]
   [leihs.inventory.server.utils.middleware_handler :refer [default-handler-fetch-resource
                                                            wrap-accept-with-image-rewrite
                                                            wrap-session-token-authenticate!]]
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
   [ring.middleware.params :refer [wrap-params]]))

(def middlewares [debug-mw/wrap-debug

                  wrap-handle-coercion-error
                  db/wrap-tx
                  core-routing/wrap-canonicalize-params-maps
                  muuntaja/format-middleware
                  ring-audits/wrap
                  wrap-accept-with-image-rewrite

                  csrf/extract-header

                  wrap-session-token-authenticate!
                  wrap-authenticate!

                  wrap-cookies
                  csrf/wrap-csrf
                  leihs.core.anti-csrf.back/wrap
                  dm/extract-dev-cookie-params

                  wrap-params
                  wrap-content-type
                  dispatch-content-type/wrap-accept
                  default-handler-fetch-resource

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
  (let [router (ring/router (routes/all-api-endpoints) default-router-config)
        swagger-ui-handler (swagger/init)
        not-found-handler (default-handler-fetch-resource custom-not-found-handler)
        default-handler (ring/routes swagger-ui-handler
                                     (ring/create-default-handler {:not-found not-found-handler}))]
    (-> (ring/ring-handler router default-handler)
        (cache-buster2/wrap-resource "public" cache-bust-options)
        (wrap-content-type {:mime-types {"svg" "image/svg+xml"}})
        (wrap-default-charset "utf-8"))))
