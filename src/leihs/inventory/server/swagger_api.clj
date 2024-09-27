(ns leihs.inventory.server.swagger-api
  (:require [clojure.java.io :as io]
            [clojure.string]
            [clojure.string :as str]
            [leihs.core.auth.session :as session]
            [leihs.core.core :refer [presence]]
            [leihs.core.db]
            [leihs.core.db :as db]
            [leihs.core.ring-audits :as ring-audits]
            [leihs.core.routing.dispatch-content-type :as dispatch-content-type]
            [leihs.inventory.server.resources.utils.session :refer [session-valid?]]
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
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.util.response :as response]))

(defn get-assets []
  (let [dirs (map io/file ["resources/public/inventory/assets"
                           "resources/public/inventory/css"
                           "resources/public/inventory/static"
                           "resources/public/inventory/js"])
        dir-map {(.getPath (io/file "resources/public/inventory/assets")) "/inventory/assets/"
                 (.getPath (io/file "resources/public/inventory/css")) "/inventory/css/"
                 (.getPath (io/file "resources/public/inventory/static")) "/inventory/static/"
                 (.getPath (io/file "resources/public/inventory/js")) "/inventory/js/"}
        mime-map {".js" "text/javascript"
                  ".css" "text/css"
                  ".svg" "image/svg+xml"}
        merged-files (apply concat (map file-seq dirs))]

    (into {}
          (for [file merged-files
                :when (.isFile file)]
            (let [full-path (.getPath file)
                  filename (.getName file)

                  uri (some (fn [[dir-path uri-prefix]]
                              (when (.startsWith full-path dir-path)
                                (str uri-prefix filename)))
                            dir-map)

                  mime-type (or (some (fn [[ext mime]]
                                        (when (str/ends-with? filename ext)
                                          mime))
                                      mime-map)
                                "application/octet-stream")]

              {uri {:file (str "public" uri)
                    :file-path full-path
                    :content-type mime-type}})))))

(defn- create-root-page []
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (str "<html><body><head><link rel=\"stylesheet\" href=\"/inventory/css/additional.css\">
       </head><div class='max-width'>
       <img src=\"/inventory/static/zhdk-logo.svg\" alt=\"ZHdK Logo\" style=\"margin-bottom:4em\" />
       <h1>Overview _> go to <a href=\"/inventory\">go to /inventory<a/></h1>"
              (slurp (io/resource "md/info.html")) "</div></body></html>")})

(def whitelisted-routes-for-ssa-response ["/inventory/models/inventory-list"])

(defn file-exists? [uri]
  (let [file-path (str "resources/public" uri)]
    (.exists (java.io.File. file-path))))

(defn custom-not-found-handler [request]
  (let [uri (:uri request)
        assets (get-assets)
        asset (get assets uri)]
    (cond
      ; TODO: activate this after /login & /logout are available
;      (not (session-valid? request)) (response/redirect "/sign-in?return-to=%2Finventory")

      (= uri "/") (create-root-page)

      (and (nil? asset) (file-exists? uri) (clojure.string/includes? uri "locales"))
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body (slurp (io/resource (str "public" uri)))}

      (and (nil? asset) (or (= uri "/inventory/") (= uri "/inventory/index.html")))
      {:status 302
       :headers {"Location" "/inventory"}
       :body ""}

      (and (nil? asset) (or (= uri "/inventory/api-docs") (= uri "/inventory/api-docs/")))
      {:status 302
       :headers {"Location" "/inventory/api-docs/index.html"}
       :body ""}

      (and (nil? asset) (= uri "/inventory")) (rh/index-html-response 200)

      (not (nil? asset)) (if asset
                           (let [{:keys [file content-type]} asset
                                 resource (io/resource file)]
                             (if resource
                               {:status 200
                                :headers {"Content-Type" content-type}
                                :body (slurp resource)}
                               (rh/index-html-response 404)))
                           (rh/index-html-response 404))

      (and (nil? asset) (some #(= % uri) whitelisted-routes-for-ssa-response))
      (rh/index-html-response 200)
      :else (rh/index-html-response 404))))

(defn default-handler-fetch-resource [handler]
  (fn [request]
    (let [accept-header (get-in request [:headers "accept"])]
      (if (.contains (str accept-header) "/json")
        (handler request)
        (custom-not-found-handler request)))))

(defn browser-request-matches-javascript? [request]
  "Returns true if the accepted type is javascript or
  if the :uri ends with .js. Note that browsers do not
  use the proper accept type for javascript script tags."
  (boolean (or (= (-> request :accept :mime) :javascript)
               (re-find #".+\.js$" (or (-> request :uri presence) "")))))

(defn wrap-dispatch-content-type
  ([handler]
   (fn [request]
     (wrap-dispatch-content-type handler request)))
  ([handler request]
   (cond
     (= (-> request :accept :mime) :json) (or (handler request)
                                              (throw (ex-info "This resource does not provide a json response."
                                                              {:status 407})))
     (and (= (-> request :accept :mime) :html)
          (#{:get :head} (:request-method request))
          (not (browser-request-matches-javascript? request))) (pr "html-requested!!!" (rh/index-html-response 409))
     :else (let [response (handler request)]
             (if (and (nil? response)
                      (not (#{:post :put :patch :delete} (:request-method request)))
                      (= (-> request :accept :mime) :html)
                      (not (browser-request-matches-javascript? request)))
               (rh/index-html-response 408)
               response)))))

(defn redirect-if-no-session
  [handler]
  (fn [request]
    (if (session-valid? request)
      (handler request)
      (response/redirect "/login?return-to=inventory"))))

(defn create-app [options]
  (let [router (ring/router

                (routes/basic-routes)

                {:conflicts nil
                 :exception pretty/exception
                 :data {:coercion reitit.coercion.spec/coercion
                        :muuntaja m/instance
                        :middleware [db/wrap-tx

                                     ; redirect-if-no-session

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
                                      ;wrap-content-type

                                      ;(core-routing/wrap-resolve-handler html/html-handler)
                                     dispatch-content-type/wrap-accept
                                      ;ring-exception/wrap

                                     default-handler-fetch-resource ;; provide resources
                                     wrap-dispatch-content-type

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
                     :urls [{:name "swagger" :url "swagger.json"}]
                     :urls.primaryName "openapi"
                     :operationsSorter "alpha"}})

          (ring/create-default-handler
           {:not-found (default-handler-fetch-resource custom-not-found-handler)}))))))
