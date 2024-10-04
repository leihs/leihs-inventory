(ns leihs.inventory.server.swagger-api
  (:require [clojure.java.io :as io]
   [clojure.java.io :as io]
   [clojure.string]
   [clojure.string :as str]
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
   [ring.util.response :as response])


  (:import (java.util.zip ZipInputStream)

   [java.util.zip ZipEntry ZipInputStream]
   [java.net JarURLConnection]
   )

  )

(def SESSION_HANDLING_ACTIVATED? true)


(def mime-map {".js" "text/javascript"
               ".css" "text/css"
               ".svg" "image/svg+xml"})

(defn get-content-type [filename]
  (or (some (fn [[ext mime]]
              (when (str/ends-with? filename ext)
                mime))
        mime-map)
    "application/octet-stream"))

(defn list-resource-files [dir]
  (let [resource-dir (io/file (io/resource dir))]
    (when (.exists resource-dir)
      (for [file (file-seq resource-dir)
            :when (.isFile file)]
        (let [filename (.getName file)
              relative-path (.getPath file)
              public-path (str "/inventory/assets/" filename)]
          {:file public-path
           :file-path relative-path
           :content-type (get-content-type filename)})))))

(def mime-map {".js" "text/javascript"
               ".css" "text/css"
               ".svg" "image/svg+xml"})

(defn get-content-type [filename]
  (or (some (fn [[ext mime]]
              (when (str/ends-with? filename ext)
                mime))
        mime-map)
    "application/octet-stream"))

(defn list-jar-files [jar-resource-path]
  (let [resource (io/resource jar-resource-path)
        jar-url (-> resource .getFile io/file .getParentFile .toURI .toURL)
        p (println ">o> resource" resource)
        p (println ">o> jar-url" jar-url)
        zip-stream (ZipInputStream. (.openStream jar-url))]
    (loop [files []]
      (let [entry (.getNextEntry zip-stream)]
        (if entry
          (recur (conj files {:file (str "/inventory/assets/" (.getName entry))
                              :file-path (.getName entry)
                              :content-type (get-content-type (.getName entry))}))
          files)))))


(defn list-all-files [dir]
  (if-let [resource (io/resource dir)]
    (let [uri (.toURI resource)]
      (if (= "file" (.getScheme uri))
        ;; Unpacked resources on filesystem
        (let [
              p (println ">o> file >>>" (.getScheme uri))
              resource-file (io/file uri)]
          (if (.isDirectory resource-file)
            (list-resource-files dir)
            (println "Not a directory.")))
        ;; Resources inside a JAR
        (list-jar-files dir)))
    (println "Directory not found.")))








(defn list-jar-files [jar-resource-path]
  (let [resource-url (io/resource jar-resource-path)]
    (if resource-url
      (let [jar-connection (cast JarURLConnection (.openConnection resource-url))
            jar-file (.getJarFile jar-connection)]
        (loop [entries (enumeration-seq (.entries jar-file))
               files []]
          (if-let [entry (first entries)]
            (let [entry-name (.getName entry)]
              ;; Filter entries to include only those that match the specified path inside the JAR
              (if (and (not (.isDirectory entry))
                    (.startsWith entry-name jar-resource-path))
                (recur (rest entries)
                  (conj files {:file (str "/inventory/assets/" entry-name)
                               :file-path entry-name
                               :content-type (get-content-type entry-name)}))
                (recur (rest entries) files)))
            files)))
      (println "Directory not found."))))

;(defn list-jar-files [jar-resource-path]
;  (let [resource (io/resource jar-resource-path)
;        jar-url (-> resource .getFile io/file .getParentFile .toURI .toURL)
;        zip-stream (ZipInputStream. (.openStream jar-url))]
;    (loop [files []]
;      (let [entry (.getNextEntry zip-stream)]
;        (if entry
;          (recur (conj files (.getName entry)))
;          files)))))
;
;(defn list-all-files [dir]
;  (if-let [resource (io/resource dir)]
;    (let [resource-file (io/file resource)]
;      (if (.isDirectory resource-file)
;        ;; Unpacked resources on filesystem
;        (file-seq resource-file)
;        ;; Resources inside a JAR
;        (list-jar-files dir)))
;    (println "Directory not found.")))

;; Example usage:

;(defn list-all-files [dir]
;  (if-let [resource (io/resource dir)]
;    (let [resource-file (io/file resource)]
;      (if (.isDirectory resource-file)
;        ;; Unpacked resources on filesystem
;        (list-resource-files dir)
;        ;; Resources inside a JAR
;        (list-jar-files dir)))
;    (println "Directory not found.")))





(defn get-assets []
  (let [
        p (println ">o> test-fetch1" (io/resource "public/inventory/css/additional.css"))
        p (println ">o> test-fetch2" (list-all-files "public"))


        dirs (map io/file ["resources/public/inventory/assets"
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




;(defn get-assets []
;  (let [dirs (map #(io/resource (str "public/inventory/" %)) ["assets"
;                                                              "css"
;                                                              "static"
;                                                              "js"])
;        dir-map (into {}
;                  (for [[dir-key uri-prefix] [["assets" "/inventory/assets/"]
;                                              ["css" "/inventory/css/"]
;                                              ["static" "/inventory/static/"]
;                                              ["js" "/inventory/js/"]]]
;                    (let [url (io/resource (str "public/inventory/" dir-key))]
;                      ;; Handle cases where resource is not found (nil)
;                      (when url
;                        [(.getFile url) uri-prefix]))))
;        mime-map {".js" "text/javascript"
;                  ".css" "text/css"
;                  ".svg" "image/svg+xml"}]
;
;    ;; Now we loop through files, ensuring that we use the file path properly
;    (into {}
;      (for [dir dirs
;            :when dir ;; Skip if dir is nil
;            :let [files (file-seq (io/file (.getFile dir)))]
;            file files
;            :when (.isFile file)]
;        (let [full-path (.getPath file)
;              filename (.getName file)
;
;              ;; Use dir-map with proper handling of the file paths
;              uri (some (fn [[dir-path uri-prefix]]
;                          (when (.startsWith full-path dir-path)
;                            (str uri-prefix filename)))
;                    dir-map)
;
;              mime-type (or (some (fn [[ext mime]]
;                                    (when (str/ends-with? filename ext)
;                                      mime))
;                              mime-map)
;                          "application/octet-stream")]
;
;          {uri {:file (str "public" uri)
;                :file-path full-path
;                :content-type mime-type}})))))




(defn- create-root-page []
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (str "<html><body><head><link rel=\"stylesheet\" href=\"/inventory/css/additional.css\">
       </head><div class='max-width'>
       <img src=\"/inventory/zhdk-logo.svg\" alt=\"ZHdK Logo\" style=\"margin-bottom:4em\" />
       <h1>Overview _> go to <a href=\"/inventory\">go to /inventory<a/></h1>"
           (slurp (io/resource "md/info.html")) "</div></body></html>")})

(def known-file-extensions #{".css" ".js" ".json" ".png" ".jpg" ".jpeg" ".gif" ".svg"})

(def whitelisted-routes-for-ssa-response ["/inventory/models/inventory-list"])

(defn file-uri?
  "Checks if the given URI ends with a file extension using a regex.
  Extensions can be like .txt, .pdf, .jpg, etc."
  [uri]
  (let [file-extension-regex #"\.(?i)(txt|pdf|jpg|jpeg|png|gif|doc|docx|xls|xlsx|csv|json|xml|html|zip|tar|gz|rar|mp3|mp4|wav)$"]
    (boolean (re-find file-extension-regex uri))))

(defn pr [str fnc]
  (println ">oo> " str fnc)
  fnc)

(defn file-request? [uri]
  (some #(str/ends-with? uri %) known-file-extensions))

(defn fetch-file-entry "Return asset-entry if file requested and uri contains no '/static/'" [uri assets]
  (if (and (file-request? uri) (not (clojure.string/includes? uri "/static/")))
    (some (fn [[key value]]
            (if (or (clojure.string/includes? (str key) uri)
                  (clojure.string/includes? (str key) (clojure.string/replace-first uri "/inventory" "")))
              value))
      assets)
    nil))

(defn custom-not-found-handler [request]
  (let [uri (:uri request)
        assets (get-assets)
        asset (fetch-file-entry uri assets)

        p (println ">o> (list-all-files \"public\")" (list-all-files "public"))



        p (println ">o> uri" uri)
        p (println ">o> assets" assets)
        p (println ">o> asset" asset)

        ]

    (cond

      (= uri "/") (create-root-page)

      (clojure.string/includes? uri "/sign-in")
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body (slurp (io/resource "public/sign-in-fallback.html"))}

      (str/starts-with? uri "/inventory/locales/")
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

      (and (nil? asset) (= uri "/inventory")) (rh/index-html-response 200)

      (not (nil? asset)) (if asset
                           (let [{:keys [file content-type]} asset
                                 resource (io/resource file)

                                 p (println ">o> asset" asset)

                                 p (println ">o> file" file)
                                 p (println ">o> content-type" content-type)
                                 ]
                             (if resource
                               {:status 200
                                :headers {"Content-Type" content-type}
                                :body (slurp resource)}
                               ;:body (slurp resource)}
                               (pr ">o> 404-1" (rh/index-html-response 404))))
                           (pr ">o> 404-2" (rh/index-html-response 404)))

      (and SESSION_HANDLING_ACTIVATED? (not (file-uri? uri)) (not (session-valid? request)))
      (response/redirect "/sign-in?return-to=%2Finventory")

      (and (nil? asset) (some #(= % uri) whitelisted-routes-for-ssa-response))
      (rh/index-html-response 200)
      :else (pr ">o> NOT-FOUND" (rh/index-html-response 404)))))

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
