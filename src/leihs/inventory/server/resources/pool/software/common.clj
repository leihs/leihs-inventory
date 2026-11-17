(ns leihs.inventory.server.resources.pool.software.common
  (:require
   [leihs.inventory.server.resources.pool.models.common :refer [filter-map-by-spec]]))

(defn- replace-nil-with-empty-string
  "Replace all nil values in a map with empty strings."
  [m]
  (into {}
        (for [[k v] m]
          [k (if (nil? v) "" v)])))

(defn sanitize-single
  [res spec]
  (-> res
      replace-nil-with-empty-string
      (filter-map-by-spec spec)))

(defn sanitize-many
  [rows]
  ;; Keep non-string nil fields (e.g. UUIDs) untouched for schema coercion.
  (mapv (fn [row]
          (update row :version #(or % "")))
        rows))


