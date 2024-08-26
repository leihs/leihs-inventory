(ns leihs.inventory.server.swagger-api
  (:require [clojure.java.io :as io]
            [clojure.string]
            [leihs.core.auth.session :as session]
            [leihs.core.db]
            [leihs.core.db :as db]
            [leihs.core.ring-audits :as ring-audits]
            [leihs.core.routing.dispatch-content-type :as dispatch-content-type]
            [leihs.inventory.server.routes :as routes]
            [leihs.inventory.server.utils.response_helper :as rh]
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
            [ring.middleware.cookies :refer [wrap-cookies]]))


(defn custom-not-found-handler [request]
  (let [uri (:uri request)

        ;uri (if (= "/inventory" uri) "/inventory/index.html" uri)

        ;; Define the mapping of URI to file path and MIME type
        assets {
                "inventory" {:file "public/inventory/index.html" :content-type "text/html"}
                ;"inventory" {:file "public/inventory/index.html" :content-type "text/html"}
                ;"/assets/index-Dh2A7FpX.js" {:file "public/inventory/assets/index-Dh2A7FpX.js" :content-type "application/javascript"}
                "/assets/index-Dh2A7FpX.js" {:file "public/inventory/assets/index-Dh2A7FpX.js" :content-type "text/javascript"}
                "/assets/index-z2lRr12x.css" {:file "public/inventory/assets/index-z2lRr12x.css" :content-type "text/css"}}
        asset (get assets uri)


        ]

    (println ">o> create-default-handler" uri)
    (println ">o> create-default-handler.asset" asset)


    (cond
      (and (nil? asset) (= uri "/inventory")) (rh/index-html-response 200)

     (not (nil? asset)    )

       ;; Check if the URI matches any predefined asset
       (if asset
         (let [{:keys [file content-type]} asset
               resource (io/resource file)
               p (println ">o> create-default-handler.resource" resource)

               p (println ">o> infos: " file content-type)
               ]                                               ;; Get the resource file path
           (if resource
             {:status 200
              :headers {"Content-Type" content-type}
              :body (slurp resource)}                          ;; Serve the file with appropriate MIME type

             ;(rh/INDEX-HTML-RESPONSE-NOT-FOUND)
             (rh/index-html-response 404)
             ))              ;; Fallback to not found
         ;(rh/INDEX-HTML-RESPONSE-NOT-FOUND)
         (rh/index-html-response 404)
         )




      )
))                 ;; Fallback to not found



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
              ;{:not-found (fn [request] rh/INDEX-HTML-RESPONSE-NOT-FOUND)})
              ;{:not-found (fn [request] rh/INDEX-HTML-RESPONSE-OK)})

              ;{:not-found custom-not-found-handler})
              {:not-found (fn [request] (custom-not-found-handler request))})

            ;{:not-found (fn [request]
            ;
            ;              (println ">o> create-default-handler" (:uri request))
            ;
            ;              rh/INDEX-HTML-RESPONSE-OK
            ;
            ;
            ;
            ;              )}

            )

          ;)
          )

        ;(wrap-resource "public"
        ;
        ;  {:cache-bust-paths ["/inventory/assets/index-z2lRr12x.css"
        ;                     "/inventory/assets/index-Dh2A7FpX.js"
        ;       "/inventory/index.html"] }
        ;  )
        ;(wrap-resource "public"
        ;  {:allow-symlinks? true
        ;   :cache-bust-paths ["/inventory/css/additional.css"
        ;                      "/inventory/js/main.js"]
        ;   :never-expire-paths [#".*fontawesome-[^\/]*\d+\.\d+\.\d+\/.*"
        ;                        #".+_[0-9a-f]{40}\..+"]
        ;   :enabled? true})

        )))
