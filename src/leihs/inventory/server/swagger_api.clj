(ns leihs.inventory.server.swagger-api
  (:require [clojure.java.io :as io]
            [clojure.string]
            [clojure.string :as str]
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

(defn get-assets []
  ;; Directory containing your assets
  (let [assets-dir (io/file "resources/public/inventory/assets")
        files (file-seq assets-dir)]
    ;; Filter the files and create a map for the assets
    (into {}
      (for [file files
            :when (.isFile file)]                           ;; Only process files, not directories
        (let [filename (.getName file)
              uri (str "/inventory/assets/" filename)
              mime-type (cond
                          (str/ends-with? filename ".js") "text/javascript"
                          (str/ends-with? filename ".css") "text/css"
                          (str/ends-with? filename ".svg") "image/svg+xml"
                          :else "application/octet-stream")

              ;file (.getPath file)
              ;p (println ">o> file" file)
              ;file (str/replace full-path #"resources/public/" "")

              ]
          ;; Map the file path and MIME type
          {uri {
                ;:file (.getPath file)
                :file uri
                :file-path (str "public/" uri)
                :content-type mime-type}})))))


(defn custom-not-found-handler [request]
  (let [uri (:uri request)

        ;uri (if (= "/inventory" uri) "/inventory/index.html" uri)

        p (println ">o> >>> uri=" uri)

        ;;; Define the mapping of URI to file path and MIME type
        ;assets {
        ;        "/inventory" {:file "public/inventory/index.html" :content-type "text/html"}
        ;        "/inventory/" {:file "public/inventory/index.html" :content-type "text/html"}
        ;        ;"inventory" {:file "public/inventory/index.html" :content-type "text/html"}
        ;        ;"/assets/index-Dh2A7FpX.js" {:file "public/inventory/assets/index-Dh2A7FpX.js" :content-type "application/javascript"}
        ;        "/inventory/assets/index-Ci25JBn3.js" {:file "public/inventory/assets/index-Ci25JBn3.js" :content-type "text/javascript"}
        ;        "/inventory/assets/index-z2lRr12x.css" {:file "public/inventory/assets/index-z2lRr12x.css" :content-type "text/css"}
        ;        "/inventory/assets/vite.svg" {:file "public/inventory/assets/zhdk-logo.svg" :content-type "text/png"}
        ;        }

        assets (get-assets)

        p (println ">o> >>> assets=" assets)

        asset (get assets uri)

        p (println ">o> >>> asset=" asset)

        ]

    (println ">o> >>> create-default-handler" uri)
    (println ">o> >>> create-default-handler.asset" asset)


    (cond
      (and (nil? asset) (or (= uri "/inventory/") (= uri "/inventory/index.html"))) {:status 302
                                                                                     :headers {"Location" "/inventory"}
                                                                                     :body ""}

      (and (nil? asset) (= uri "/inventory")) (rh/index-html-response 200)

      ;; Check if the URI matches any predefined asset
      (not (nil? asset)) (if asset
                           (let [{:keys [file-path file content-type]} asset

                                 p (println ">o> get-resource.file=" file)

                                 ;resource (io/resource (str "public/" file))
                                 resource (io/resource file-path)
                                 p (println ">o> create-default-handler.resource" resource)

                                 p (println ">o> infos: " file content-type)
                                 ]                          ;; Get the resource file path
                             (if resource
                               {:status 200
                                :headers {"Content-Type" content-type}
                                :body (slurp resource)}     ;; Serve the file with appropriate MIME type

                               ;(rh/INDEX-HTML-RESPONSE-NOT-FOUND)
                               (rh/index-html-response 404)
                               ))                           ;; Fallback to not found
                           ;(rh/INDEX-HTML-RESPONSE-NOT-FOUND)
                           (rh/index-html-response 404)
                           )

      )
    ))                                                      ;; Fallback to not found



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
