(ns leihs.inventory.server.utils.helper
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [taoensso.timbre :refer [error debug]]))

(defn- ->snake-case
  "Converts a string `s` to snake_case."
  [s]
  ;; 1. Insert underscores before any capital letters in CamelCase.
  ;; 2. Lower-case everything.
  ;; 3. Replace dashes with underscores.
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
                 ;; k might be a keyword, symbol, or string, so coerce to string first
                    [(keyword (->snake-case (name k))) v]))
             x)

       :else x))
   data))

(defn convert-to-map [dict]
  (into {} (map (fn [[k v]] [(clojure.core/keyword k) v]) dict)))
