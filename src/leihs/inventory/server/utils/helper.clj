(ns leihs.inventory.server.utils.helper
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [taoensso.timbre :refer [warn]])
  (:import
   (java.util UUID)))

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

(defn accept-header-html? [request]
  (let [accept-header (get-in request [:headers "accept"])]
    (and accept-header (str/includes? accept-header "text/html"))))

(defn to-uuid
  ([value]
   (try
     (let [result (if (instance? String value) (UUID/fromString value) value)]
       result)
     (catch Exception e
       (warn "DEV-ERROR in to-uuid[value], value=" value ", exception=" (.getMessage e))
       value)))

  ([value key]
   (def keys-to-cast-to-uuid #{:user_id :id :group_id :person_id :collection_id :media_entry_id :accepted_usage_terms_id :delegation_id
                               :uploader_id :created_by_id
                               :keyword_id})
   (let [res (try
               (if (and (contains? keys-to-cast-to-uuid (keyword key)) (instance? String value))
                 (UUID/fromString value)
                 value)
               (catch Exception e
                 (warn ">>> DEV-ERROR in to-uuid[value key], value=" value ", key=" key " exception=" (.getMessage e))
                 value))] res))

  ([value key table]
   (def blacklisted-tables #{"meta_keys" "vocabularies"})

   ;; XXX: To fix db-exceptions of io_interfaces
   (if (or (contains? blacklisted-tables (name table)) (and (= table :io_interfaces) (= key :id)))
     value
     (to-uuid value key))))

(def uuid-regex
  #"([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})$")

(defn url-ends-with-uuid? [url]
  (let [path (first (str/split url #"\?"))
        segments (str/split path #"/")
        last-segment (last segments)]
    (boolean (re-matches uuid-regex last-segment))))
