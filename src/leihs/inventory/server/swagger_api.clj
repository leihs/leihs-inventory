(ns leihs.inventory.server.swagger-api
  (:require
   [leihs.core.anti-csrf.back :as anti-csrf]
   [leihs.core.db :as db]
   [leihs.core.ring-audits :as ring-audits]
   [leihs.core.routing.back :as core-routing]
   [leihs.core.routing.dispatch-content-type :as dispatch-content-type]
   [leihs.inventory.server.resources.routes :as routes]
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
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.cookies :refer [wrap-cookies]]
   [ring.middleware.params :refer [wrap-params]]))

(def middlewares [wrap-handle-coercion-error
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

                  swagger/swagger-feature
                  parameters/parameters-middleware
                  muuntaja/format-negotiate-middleware
                  muuntaja/format-response-middleware

                  exception/exception-middleware
                  debug-mw/wrap-debug

                  muuntaja/format-request-middleware
                  coercion/coerce-response-middleware
                  coercion/coerce-request-middleware
                  multipart/multipart-middleware])

(defn create-app [options]
  (let [router (ring/router
                (routes/all-api-endpoints)
                {:conflicts nil
                 :strict-slash true
                 :exception pretty/exception
                 :data {:coercion reitit.coercion.spec/coercion
                        :muuntaja m/instance
                        :middleware middlewares}})]

    (-> (ring/ring-handler
         router
         (ring/routes

          (swagger-ui/create-swagger-ui-handler
           {:path "/inventory/api-docs/"
            :root "swagger-ui"
            :config {:validatorUrl nil
                     :urls [{:name "swagger" :url "swagger.json"}]
                     :urls.primaryName "openapi"
                     :operationsSorter "alpha"}})

          (ring/create-default-handler
           {:not-found (default-handler-fetch-resource custom-not-found-handler)}))))))
