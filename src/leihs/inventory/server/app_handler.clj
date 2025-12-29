(ns leihs.inventory.server.app-handler
  (:require
   [leihs.core.anti-csrf.back :as anti-csrf]
   [leihs.core.db :as db]
   [leihs.core.http-cache-buster2 :as cache-buster2]
   [leihs.core.ring-audits :as ring-audits]
   [leihs.core.routing.back :as core-routing]
   [leihs.core.routing.dispatch-content-type :as dispatch-content-type]
   [leihs.core.settings :as settings]
   [leihs.inventory.server.resources.routes :as routes]
   [leihs.inventory.server.swagger :as swagger]
   [leihs.inventory.server.utils.coercion :refer [wrap-handle-coercion-error]]
   [leihs.inventory.server.utils.csrf-handler :as csrf]
   [leihs.inventory.server.utils.debug :as debug-mw]
   [leihs.inventory.server.utils.exception-handler :refer [wrap-exception]]
   [leihs.inventory.server.utils.middleware-handler :refer [wrap-html-40x
                                                            wrap-session-token-authenticate!
                                                            wrap-strict-format-negotiate]]
   [leihs.inventory.server.utils.ressource-handler :refer [custom-not-found-handler]]
   [muuntaja.core :as m]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [reitit.dev.pretty :as pretty]
   [reitit.ring :as reitit-ring]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [reitit.swagger]
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.cookies :refer [wrap-cookies]]
   [ring.middleware.default-charset :refer [wrap-default-charset]]
   [ring.middleware.params :refer [wrap-params]]))

(def middlewares [debug-mw/wrap-debug
                  #(wrap-html-40x % [#"/inventory/.+"])
                  muuntaja/format-response-middleware
                  wrap-exception

                  wrap-strict-format-negotiate
                  wrap-handle-coercion-error
                  db/wrap-tx
                  settings/wrap
                  core-routing/wrap-canonicalize-params-maps
                  muuntaja/format-middleware
                  ring-audits/wrap

                  csrf/extract-header

                  wrap-session-token-authenticate!

                  wrap-cookies
                  csrf/wrap-csrf
                  leihs.core.anti-csrf.back/wrap

                  wrap-params
                  wrap-content-type
                  dispatch-content-type/wrap-accept

                  reitit.swagger/swagger-feature
                  parameters/parameters-middleware
                  muuntaja/format-negotiate-middleware
                  muuntaja/format-response-middleware

                  muuntaja/format-request-middleware
                  coercion/coerce-response-middleware
                  coercion/coerce-request-middleware
                  multipart/multipart-middleware])

(def cache-bust-options
  {:cache-bust-paths [#"^/inventory/assets/.*\.(js|css|png|jpg|svg|woff2?)$"]
   :never-expire-paths []
   :cache-enabled? true})

(def default-router-config
  {:conflicts nil
   :strict-slash true
   :exception pretty/exception
   :data {:coercion reitit.coercion.spec/coercion
          :muuntaja m/instance
          :middleware middlewares}})

(def default-handler
  (reitit-ring/create-default-handler {:not-found custom-not-found-handler
                                       :method-not-allowed custom-not-found-handler}))

(defn- wrap-attach-router-to-request [handler router]
  (fn [request]
    (handler (assoc request :reitit.router router))))

(defn init []
  (let [router (reitit-ring/router (routes/all-api-endpoints)
                                   default-router-config)]
    (-> router
        (reitit-ring/ring-handler default-handler)
        (->> (reitit-ring/routes (swagger/init)))
        (wrap-attach-router-to-request router)
        (cache-buster2/wrap-resource "public" cache-bust-options)
        (wrap-content-type {:mime-types {"svg" "image/svg+xml"}})
        (wrap-default-charset "utf-8"))))
