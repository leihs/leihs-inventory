(ns leihs.inventory.server.resources.pool.list.helper
  (:require
   [clojure.string :as str]  )
  (:import [java.time Instant]))

(defn prepare-filters [filters]
  (cond-> filters
    (contains? filters :retired)
    (assoc :retired (case (:retired filters)
                      true  (Instant/now)
                      false nil
                      (:retired filters)))))

(defn extract-ids [fields prefix-to-remove]
  (let [ids (keep :id fields)
        prefixed? #(str/starts-with? (name %) prefix-to-remove)
        properties (filter prefixed? ids)
        keys (mapv (fn [id]
                     (let [id-str (name id)]
                       (-> id-str
                         (str/replace
                           (re-pattern (str "^" (java.util.regex.Pattern/quote prefix-to-remove)))
                           "")
                         keyword)))
               ids)]
    {:filter-keys keys
     :properties (vec properties)
     :raw-filter-keys (vec ids)}))
