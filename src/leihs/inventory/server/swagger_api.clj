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
  (let [assets-dir (io/file "resources/public/inventory/assets")
        css-dir (io/file "resources/public/inventory/css")
        files (file-seq assets-dir)
        files2 (file-seq css-dir)
        merged-files (concat files files2)]

    (into {}
          (for [file merged-files
                :when (.isFile file)]
            (let [full-path (.getPath file)
                  filename (.getName file)

                  uri (cond
                        (.startsWith full-path (.getPath assets-dir)) (str "/inventory/assets/" filename)
                        (.startsWith full-path (.getPath css-dir)) (str "/inventory/css/" filename))

                  mime-type (cond
                              (str/ends-with? filename ".js") "text/javascript"
                              (str/ends-with? filename ".css") "text/css"
                              (str/ends-with? filename ".svg") "image/svg+xml"
                              :else "application/octet-stream")]
              {uri {:file uri
                    :file-path full-path
                    :content-type mime-type}})))))

(defn custom-not-found-handler [request]
  (let [uri (:uri request)
        assets (get-assets)
        asset (get assets uri)]

    (cond
      (and (nil? asset) (or (= uri "/inventory/") (= uri "/inventory/index.html"))) {:status 302
                                                                                     :headers {"Location" "/inventory"}
                                                                                     :body ""}

      (and (nil? asset) (= uri "/inventory")) (rh/index-html-response 200)

      (not (nil? asset)) (if asset
                           (let [{:keys [file content-type]} asset
                                 resource (io/resource (str "public/" file))]
                             (if resource
                               {:status 200
                                :headers {"Content-Type" content-type}
                                :body (slurp resource)}

                               (rh/index-html-response 404)))
                           (rh/index-html-response 404)))))

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
           {:not-found (fn [request] (custom-not-found-handler request))}))))))
