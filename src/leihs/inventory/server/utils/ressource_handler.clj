(ns leihs.inventory.server.utils.ressource-handler
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.walk :refer [keywordize-keys]]
   [leihs.core.auth.session :as session]
   [leihs.core.db :as db]
   [leihs.inventory.server.resources.auth.session :refer [get-cookie-value]]
   [leihs.inventory.server.resources.utils.session :refer [session-valid?]]
   [leihs.inventory.server.utils.csrf-handler :as csrf]
   [leihs.inventory.server.utils.helper :refer [accept-header-html?]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [leihs.inventory.server.utils.ressource-loader :refer [list-files-in-dir]]
   [leihs.inventory.server.utils.session-dev-mode :as dm]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.util.codec :as codec]
   [ring.util.response :as response]))

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
(def ALLOWED_RESOURCE_PATHS ["public/inventory/assets/css"
                             "public/inventory/assets/js"
                             "public/inventory/assets"])
(def RESOURCE_DIR_URI_MAP (into {} (map (fn [path] [path (str "/" (str/replace path #"public/" ""))]) ALLOWED_RESOURCE_PATHS)))
(def RESOURCE_FILES (apply concat (map list-files-in-dir ALLOWED_RESOURCE_PATHS)))
(def SUPPORTED_LOCALES ["/en/" "/de/" "/es/" "/fr/"])

(defn file-request?
  [uri]
  (some #(clojure.string/ends-with? uri %) (keys SUPPORTED_MIME_TYPES)))

(defn- create-root-page []
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (str "<html><body><head><link rel=\"stylesheet\" href=\"/inventory/assets/css/additional.css\">
       </head><div class='max-width'>
       <img src=\"/inventory/assets/zhdk-logo.svg\" alt=\"ZHdK Logo\" style=\"margin-bottom:4em\" />
       <h1>Overview _> go to <a href=\"/inventory\">go to /inventory<a/></h1>"
              (slurp (io/resource "md/info.html")) "</div></body></html>")})

(defn fetch-file-entry [uri assets]
  (if (and (file-request? uri) (clojure.string/includes? uri "/inventory/assets/"))
    (some (fn [[key value]]
            (if (.endsWith (str key) uri)
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

;; TODO: remove DEV-FORMS-HANDLING if not used anymore (start)
(defn extract-filetype [uri]
  (when-let [filename (last (str/split uri #"/"))]
    (second (re-matches #".*\.([a-zA-Z0-9]+)$" filename))))

(defn extract-filename [uri]
  (let [filename (last (str/split uri #"/"))]
    (if (and (not (empty? filename)) (re-matches #".*\.(css|js)$" filename))
      filename
      nil)))

(def allowed-types #{"model" "software" "license" "item" "option" "package"})

(defn generate-content-type [filetype]
  (let [charset "; charset=utf-8"]
    (if (= filetype "js")
      (str "application/javascript" charset)
      (str "text/" filetype charset))))

(defn custom-not-found-handler [request]
  (let [request ((db/wrap-tx (fn [request] request)) request)
        request ((csrf/extract-header (fn [request] request)) request)
        request ((session/wrap-authenticate (fn [request] request)) request)
        request ((dm/extract-dev-cookie-params (fn [request] request)) request)
        uri (:uri request)
        file (extract-filename uri)
        assets (get-assets)
        asset (fetch-file-entry uri assets)]

    (cond
      (= uri "/") (create-root-page)

      ;; TODO: DEV-ENDPOINT
      (and (dm/has-admin-permission request) (str/starts-with? uri "/inventory/dev/") (not (nil? file)))
      {:status 200
       :headers {"Content-Type" (generate-content-type (extract-filetype uri))}
       :body (slurp (io/resource (str "public/dev/" file)))}

      (and (dm/has-admin-permission request) (re-matches #"/inventory/[a-f0-9\-]+/dev/([a-z]+)" uri))
      (let [type (second (re-find #"/inventory/[a-f0-9\-]+/dev/([a-z]+)" uri))]
        (if (allowed-types type)
          {:status 200
           :headers {"Content-Type" "text/html"}
           :body (slurp (io/resource (str "public/dev/create-" type ".html")))}
          {:status 400
           :body "Invalid type"}))

      (and (str/starts-with? uri "/inventory/assets/locales/") (str/ends-with? uri "/translation.json")
           (contains-one-of? uri SUPPORTED_LOCALES))
      (let [src (str/replace-first uri "/inventory" "public/inventory")]
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

      (and (nil? asset) (accept-header-html? request)) (rh/index-html-response request 200)

      :else (rh/index-html-response request 404))))
