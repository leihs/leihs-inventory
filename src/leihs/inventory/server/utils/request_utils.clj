(ns leihs.inventory.server.utils.request-utils
  (:require
   [clojure.string]
   [ring.util.codec :as codec]))

(defn parse-fields
  "Parses a URL-encoded query parameter like 'fields=field1%2Cfield2%2Cfield3' and returns a vector of fields."
  [fields-param]
  (when (and fields-param (not (clojure.string/blank? fields-param)))
    (-> fields-param
        codec/url-decode ; Decode the URL-encoded string
        (clojure.string/split #",") ; Split by commas
        vec))) ; Convert to a vector

(defn pick-fields
  "xs: vector of maps, or a map containing :data (with optional :pagination etc.).
   fields: vector of strings or keywords, e.g. [\"is_package\" \"name\"].
   Returns the same structure but each item map only contains the wanted fields."
  [xs fields]
  (let [ks (set (map #(if (keyword? %) % (keyword %))
                     (parse-fields fields)))
        pick (fn [m] (select-keys m ks))
        process (fn [x]
                  (cond
                    ;; wrapped form: {:data [...] ...}
                    (and (map? x) (vector? (:data x)))
                    (update x :data #(mapv pick %))

                    ;; direct item map
                    (map? x) (pick x)

                    :else x))] ; unexpected case: leave unchanged
    (if (seq fields)
      (cond
        (map? xs) (process xs)
        (sequential? xs) (mapv process xs)
        :else xs)
      xs)))

(def AUTHENTICATED_ENTITY :authenticated-entity)

(defn authenticated? [request]
  (-> request
      AUTHENTICATED_ENTITY
      boolean))

(defn get-auth-entity [request]
  (-> request
      AUTHENTICATED_ENTITY))

(defn path-params [request]
  (-> request :parameters :path))

(defn query-params [request]
  (-> request :parameters :query))


