(ns leihs.inventory.server.utils.ressource-loader
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [leihs.core.auth.session :as session]
            [leihs.core.core :refer [presence]]
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
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.util.response :as response])
  (:import [java.net URL JarURLConnection]
           [java.util.jar JarFile]))

;; Workaround to list resource-files from jar and local resource-dir
(defn- jar-file-entries [jar-file path]
  (let [entries (enumeration-seq (.entries jar-file))]
    (filter #(str/starts-with? (.getName %) path) entries)))

(defn- list-files-in-jar [jar-url path]
  (let [jar-conn (.openConnection jar-url)
        jar-file-path (-> jar-conn
                          .getJarFileURL
                          .getPath
                          (str/replace #"^file:" ""))
        jar-file (JarFile. jar-file-path)]
    (->> (jar-file-entries jar-file path)
         (filter #(not (.isDirectory %)))
         (map #(.getName %)))))

(defn list-files-in-dir [dir-uri]
  (if-let [resource-url (io/resource dir-uri)]
    (let [protocol (.getProtocol resource-url)]
      (cond
        (= protocol "file")
        (->> (file-seq (io/file resource-url))
             (filter #(.isFile %)))

        (= protocol "jar")
        (list-files-in-jar resource-url dir-uri)))))
