(ns leihs.inventory.server.utils.ressource-handler
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [leihs.core.auth.session :as session]
            [leihs.core.core :refer [presence]]
            [leihs.core.db :as db]
            [leihs.core.ring-audits :as ring-audits]
            [leihs.core.routing.dispatch-content-type :as dispatch-content-type]
            [leihs.core.sign-in.back :as be]
            [leihs.core.sign-in.simple-login :refer [sign-in-view]]
            [leihs.inventory.server.resources.utils.session :refer [session-valid?]]
            [leihs.inventory.server.routes :as routes]
            [leihs.inventory.server.utils.csrf-handler :as csrf]
            [leihs.inventory.server.utils.response_helper :as rh]
            [leihs.inventory.server.utils.ressource-loader :refer [list-files-in-dir]]
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
            [ring.util.response :as response])
  (:import [java.net URL JarURLConnection]
           [java.util.jar JarFile]))

(def SESSION_HANDLING_ACTIVATED? true)
(def WHITELISTED_ROUTES_FOR_SSA_RESPONSE ["/inventory/models/inventory-list"])
(def SUPPORTED_MIME_TYPES {".js" "text/javascript"
                           ".css" "text/css"
                           ".svg" "image/svg+xml"
                           ".json" "application/json"
                           ".png" "image/png"
                           ".jpg" "image/jpeg"
                           ".jpeg" "image/jpeg"
                           ".gif" "image/gif"})
(def ALLOWED_RESOURCE_PATHS ["public/inventory/assets"
                             "public/inventory/css"
                             "public/inventory/static"
                             "public/inventory/js"])
(def RESOURCE_DIR_URI_MAP (into {} (map (fn [path] [path (str "/" (str/replace path #"public/" ""))]) ALLOWED_RESOURCE_PATHS)))
(def RESOURCE_FILES (apply concat (map list-files-in-dir ALLOWED_RESOURCE_PATHS)))
(def SUPPORTED_LOCALES ["/en/" "/de/" "/es/" "/fr/"])

(defn file-request?
  [uri]
  (some #(clojure.string/ends-with? uri %) (keys SUPPORTED_MIME_TYPES)))

(defn- create-root-page []
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (str "<html><body><head><link rel=\"stylesheet\" href=\"/inventory/css/additional.css\">
       </head><div class='max-width'>
       <img src=\"../../inventory/zhdk-logo.svg\" alt=\"ZHdK Logo\" style=\"margin-bottom:4em\" />
       <h1>Overview _> go to <a href=\"/inventory\">go to /inventory<a/></h1>"
              (slurp (io/resource "md/info.html")) "</div></body></html>")})

(defn fetch-file-entry [uri assets]
  (if (and (file-request? uri) (not (clojure.string/includes? uri "/static/")))
    (some (fn [[key value]]
            (if (or (clojure.string/includes? (str key) uri)
                    (clojure.string/includes? (str key) (clojure.string/replace-first uri "/inventory" "")))
              value))
          assets)
    nil))

(defn get-assets []
  (into {}
        (for [file RESOURCE_FILES]
          (let [file2 (clojure.java.io/file file)
                filename (.getName file2)
                full-path file
                uri (some (fn [[dir-path uri-prefix]]
                            (when (str/includes? full-path dir-path)
                              (str uri-prefix "/" filename)))
                          RESOURCE_DIR_URI_MAP)
                mime-type (or (some (fn [[ext mime]]
                                      (when (str/ends-with? filename ext)
                                        mime))
                                    SUPPORTED_MIME_TYPES)
                              "application/octet-stream")]
            {uri {:file (str "public" uri)
                  :file-path full-path
                  :content-type mime-type}}))))

(defn contains-one-of? [s substrings]
  (some #(str/includes? s %) substrings))

(defn custom-not-found-handler [request]
  (let [request ((db/wrap-tx (fn [request] request)) request)
        request ((csrf/extract-header (fn [request] request)) request)
        request ((session/wrap-authenticate (fn [request] request)) request)
        uri (:uri request)
        assets (get-assets)
        asset (fetch-file-entry uri assets)]
    (cond
      (= uri "/") (create-root-page)

      ;; TODO: DEV-ENDPOINT
      (= uri "/inventory/8bd16d45-056d-5590-bc7f-12849f034351/dev/model") {:status 200
                                                                           :headers {"Content-Type" "text/html"}
                                                                           :body (slurp (io/resource "public/dev/create-model.html"))}

      (and (str/starts-with? uri "/inventory/locales/") (contains-one-of? uri SUPPORTED_LOCALES))
      (let [src (str/replace-first uri "/inventory" "public/inventory/static")]
        {:status 200
         :headers {"Content-Type" "application/json"}
         :body (slurp (io/resource src))})

      (and (nil? asset) (or (= uri "/inventory/") (= uri "/inventory/index.html")))
      {:status 302
       :headers {"Location" "/inventory"}
       :body ""}

      (and (nil? asset) (or (= uri "/inventory/api-docs") (= uri "/inventory/api-docs/")))
      {:status 302
       :headers {"Location" "/inventory/api-docs/index.html"}
       :body ""}

      (not (nil? asset)) (if asset
                           (let [{:keys [file content-type]} asset
                                 resource (io/resource file)]
                             (if resource
                               {:status 200
                                :headers {"Content-Type" content-type}
                                :body (slurp resource)}
                               (rh/index-html-response request 404)))
                           (rh/index-html-response request 404))

      (and SESSION_HANDLING_ACTIVATED? (not (file-request? uri)) (not (session-valid? request)))
      (response/redirect "/sign-in?return-to=%2Finventory")

      (and (nil? asset) (some #(= % uri) WHITELISTED_ROUTES_FOR_SSA_RESPONSE))
      (rh/index-html-response request 200)

      :else (rh/index-html-response request 404))))
