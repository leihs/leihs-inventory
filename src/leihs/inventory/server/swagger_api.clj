(ns leihs.inventory.server.swagger-api
  (:require [clojure.string]
            [leihs.core.auth.session :as session]
            [leihs.core.db]
            [leihs.core.db :as db]
            [leihs.core.ring-audits :as ring-audits]
            [leihs.inventory.server.routes :as routes]
            [leihs.inventory.server.utils.response_helper :as rh]
            [muuntaja.core :as m]
            [reitit.coercion.schema]
            [reitit.coercion.spec]
            [reitit.dev.pretty :as pretty]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as coercion]

            [ring.middleware.content-type :refer [wrap-content-type]]
            [leihs.core.routing.dispatch-content-type :as dispatch-content-type]

            [reitit.ring.middleware.exception :as exception]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.resource :refer [wrap-resource]]))

(defn create-app [options]
  (let [router (ring/router

                 (routes/basic-routes)

                 {:conflicts nil
                  :exception pretty/exception
                  :data {:coercion reitit.coercion.spec/coercion
                         :muuntaja m/instance
                         :middleware [db/wrap-tx

                                      ring-audits/wrap
                                      ;anti-csrf/wrap
                                      session/wrap-authenticate
                                      wrap-cookies

                                      ;locale/wrap
                                      ;settings/wrap
                                      ;datasource/wrap-tx
                                      ;wrap-json-response
                                      ;(wrap-json-body {:keywords? true})
                                      ;wrap-empty
                                      ;core-routing/wrap-canonicalize-params-maps
                                      ;wrap-params
                                      ;wrap-multipart-params
                                      wrap-content-type
                                      ;(core-routing/wrap-resolve-handler html/html-handler)
                                      dispatch-content-type/wrap-accept
                                      ;ring-exception/wrap

                                      swagger/swagger-feature
                                      parameters/parameters-middleware
                                      muuntaja/format-negotiate-middleware
                                      muuntaja/format-response-middleware
                                      exception/exception-middleware
                                      muuntaja/format-request-middleware
                                      coercion/coerce-response-middleware
                                      coercion/coerce-request-middleware
                                      multipart/multipart-middleware]}})]

    (-> (ring/ring-handler
          router
          (ring/routes
            (ring/redirect-trailing-slash-handler {:method :strip})

            (swagger-ui/create-swagger-ui-handler
              {:path "/inventory/api-docs/"
               :config {:validatorUrl nil
                        :urls [;; TODO: revise config to support multiple specs/accept-types
                               ;{:name "openapi" :url "openapi.json"}
                               {:name "swagger" :url "swagger.json"}]
                        :urls.primaryName "openapi"
                        :operationsSorter "alpha"}})



            (ring/create-default-handler
             {:not-found (fn [request] rh/INDEX-HTML-RESPONSE-NOT-FOUND)})

            )
          )

        (wrap-resource "public/inventory"

          {:cache-bust-paths ["/inventory/assets/index-z2lRr12x.css"
                             "/inventory/assets/index-Dh2A7FpX.js"
               "/inventory/index.html"] }
          )
        ;(wrap-resource "public"
        ;  {:allow-symlinks? true
        ;   :cache-bust-paths ["/inventory/css/additional.css"
        ;                      "/inventory/js/main.js"]
        ;   :never-expire-paths [#".*fontawesome-[^\/]*\d+\.\d+\.\d+\/.*"
        ;                        #".+_[0-9a-f]{40}\..+"]
        ;   :enabled? true})

        )))
