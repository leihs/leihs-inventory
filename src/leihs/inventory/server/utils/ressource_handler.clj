(ns leihs.inventory.server.utils.ressource-handler
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [leihs.inventory.server.utils.helper :refer [accept-header-html?]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [leihs.inventory.server.utils.ressource-loader :refer [list-files-in-dir]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.util.response :refer [response status content-type]]))

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
(def CONST_SUPPORTED_LOCALES ["/en/" "/de/" "/es/" "/fr/"])

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
            (if (str/includes? uri (str key))
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

(defn create-not-found-response [request]
  (let [accept-header (or (get-in request [:headers "accept"]) "")]
    (if (clojure.string/includes? accept-header "application/json")
      (-> {:status "failure" :message "No entry found"}
          (json/generate-string)
          (response)
          (status 404)
          (content-type "application/json; charset=utf-8"))
      (rh/index-html-response request 404))))

(defn custom-not-found-handler [request]
  (let [uri (:uri request)
        assets (get-assets)
        asset (fetch-file-entry uri assets)]

    (cond
      (= uri "/") (create-root-page)

      (and (str/starts-with? uri "/inventory/assets/locales/") (str/ends-with? uri "/translation.json")
           (contains-one-of? uri CONST_SUPPORTED_LOCALES))
      (let [src (str/replace-first uri "/inventory" "public/inventory")
            resource (try (slurp (io/resource src))
                          (catch Exception _ nil))]
        (if resource
          {:status 200 :headers {"Content-Type" "application/json"} :body resource}
          {:status 404 :headers {"Content-Type" "application/json"}}))

      asset (let [{:keys [file content-type]} asset
                  resource (io/resource file)]
              (if resource
                {:status 200 :headers {"Content-Type" content-type} :body (slurp resource)}
                (rh/index-html-response request 404)))

      (and (nil? asset) (accept-header-html? request))
      (rh/index-html-response request 200)

      :else (create-not-found-response request))))
