(ns leihs.inventory.server.utils.ressource-handler
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [leihs.core.auth.session :as session]
   [leihs.core.db :as db]
   [leihs.inventory.server.utils.csrf-handler :as csrf]
   [leihs.inventory.server.utils.helper :refer [accept-header-html?]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [leihs.inventory.server.utils.ressource-loader :refer [list-files-in-dir]]
   [leihs.inventory.server.utils.session-dev-mode :as dm]
   [leihs.inventory.server.utils.session-utils :refer [session-valid?]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.util.response :as response]))

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

(defn extract-filename [uri]
  (let [filename (last (str/split uri #"/"))]
    (if (and (not (empty? filename)) (re-matches #".*\.(css|js)$" filename))
      filename
      nil)))

(defn custom-not-found-handler [request]
  (let [request ((db/wrap-tx (fn [request] request)) request)
        request ((csrf/extract-header (fn [request] request)) request)
        request ((session/wrap-authenticate (fn [request] request)) request)
        request ((dm/extract-dev-cookie-params (fn [request] request)) request)
        uri (:uri request)
        file (extract-filename uri)
        assets (get-assets)
        asset (fetch-file-entry uri assets)
        accept-header (or (get-in request [:headers "accept"]) "")
        referer (or (get-in request [:headers "referer"]) "")
        swagger-call? (.endsWith "/inventory/api-docs/index.html" referer)
        p (println ">o> ---------------------------------------")
        p (println ">o> abc.accept wtf" accept-header )
        p (println ">o> abc.referer" referer swagger-call?)
        p (println ">o> abc.swagger-call?" swagger-call? )
        accept-html? (clojure.string/includes? accept-header "text/html")



        p (println ">o> abc.uri" uri)
        p (println ">o> abc.accept" accept-header accept-html?)
        p (println ">o> abc1" (get-in request [:headers "referer"]))



        p (println ">o> abc2" (and (not (file-request? uri)) (not (session-valid? request)) (not swagger-call?)))
        p (println ">o> abc.a"  (not (file-request? uri)) )
        p (println ">o> abc.b"  (not (session-valid? request)) )
        p (println ">o> abc.c" (not swagger-call?))
        ]

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

      (and (nil? asset) (or (= uri "/inventory/") (= uri "/inventory/index.html")))
      {:status 302 :headers {"Location" "/inventory"} :body ""}

      (and (nil? asset) (or (= uri "/inventory/api-docs") (= uri "/inventory/api-docs/")))
      {:status 302 :headers {"Location" "/inventory/api-docs/index.html"} :body ""}

      asset (let [{:keys [file content-type]} asset
                  resource (io/resource file)]
              (if resource
                {:status 200 :headers {"Content-Type" content-type} :body (slurp resource)}
                (rh/index-html-response request 404)))

      ;(and (not (file-request? uri)) (not (session-valid? request)) (not swagger-call?))
      (and accept-html? (not (session-valid? request)) (not swagger-call?))
      ;(response/redirect "/sign-in?return-to=%2Finventory")
      {:status 303 :headers {"Location" "/sign-in?return-to=%2Finventory" "Content-Type" "text/html"} :body ""}

      (and (nil? asset) (some #(= % uri) WHITELISTED_ROUTES_FOR_SSA_RESPONSE))
      (rh/index-html-response request 200)

      (and (nil? asset) (accept-header-html? request))
      (rh/index-html-response request 200)

      :else (rh/index-html-response request 404))))
