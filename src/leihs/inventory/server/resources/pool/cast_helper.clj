(ns leihs.inventory.server.resources.pool.cast-helper
  (:require
   [taoensso.timbre :refer [debug error]])
  (:import [java.math BigDecimal RoundingMode]))

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
       (catch Exception e
         (debug e)
         (error "Error in int-to-numeric" e) nil)))

(defn double-to-numeric-or-nil [int-value]
  (cond
    (nil? int-value) nil
    (instance? java.lang.Double int-value) int-value
    (customized-empty? int-value) nil
    :else (let [parsed-value (if (string? int-value)
                               (try
                                 (Double/parseDouble int-value)
                                 (catch NumberFormatException e
                                   (debug e)
                                   nil))
                               int-value)]
            (int-to-numeric-or-nil parsed-value))))
