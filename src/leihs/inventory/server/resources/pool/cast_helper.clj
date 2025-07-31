(ns leihs.inventory.server.resources.pool.cast-helper
  (:require
   [cheshire.core :as cjson]
   [cheshire.core :as jsonc]
   [clojure.data.codec.base64 :as b64]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]])
  (:import [java.math BigDecimal RoundingMode]
           [java.net URL JarURLConnection]
           (java.time LocalDateTime)
           [java.util UUID]
           [java.util.jar JarFile]))

(defn- customized-empty? [value]
  (or (= value "null")
      (nil? value)
      (and (or (string? value)
               (coll? value)
               (map? value)
               (sequential? value))
           (empty? value))))

(defn int-to-numeric-or-nil [int-value]
  (try (-> (BigDecimal/valueOf int-value) (.setScale 2 RoundingMode/HALF_UP))
       (catch Exception e (error "Error in int-to-numeric" e) nil)))

(defn double-to-numeric-or-nil [int-value]
  (cond
    (nil? int-value) nil
    (instance? java.lang.Double int-value) int-value
    (customized-empty? int-value) nil
    :else (let [parsed-value (if (string? int-value)
                               (try
                                 (Double/parseDouble int-value)
                                 (catch NumberFormatException _ nil))
                               int-value)]
            (int-to-numeric-or-nil parsed-value))))
