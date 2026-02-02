(ns leihs.inventory.server.utils.transform
  "Data transformation utilities."
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [taoensso.timbre :refer [debug]])
  (:import
   [java.util UUID]))

(defn- ->snake-case
  "Converts a string `s` to snake_case."
  [s]
  (-> s
      (str/replace #"([A-Z])" "_$1")
      str/lower-case
      (str/replace #"-" "_")
      (str/replace #"^_" "")))

(defn snake-case-keys
  "Recursively walk `data` (maps, vectors, etc.) and convert all map keys to snake_case keywords."
  [data]
  (walk/postwalk
   (fn [x]
     (cond
       (map? x)
       (into {}
             (map (fn [[k v]]
                    [(keyword (->snake-case (name k))) v]))
             x)

       :else x))
   data))

(defn convert-to-map [dict]
  (into {} (map (fn [[k v]] [(clojure.core/keyword k) v]) dict)))

(defn to-uuid [value]
  (try
    (if (instance? String value) (UUID/fromString value) value)
    (catch Exception e
      (debug e)
      value)))

(defn merge-by-id
  "Merge two vectors of maps by matching :id.
     Fields from v2 override those from v1 on collision."
  [v1 v2]
  (let [m2 (into {} (map (juxt :id identity) v2))]
    (mapv #(merge % (get m2 (:id %))) v1)))
