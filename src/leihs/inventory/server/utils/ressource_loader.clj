(ns leihs.inventory.server.utils.ressource-loader
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [reitit.coercion.schema]
   [reitit.coercion.spec])
  (:import
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
