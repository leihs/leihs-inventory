(ns leihs.inventory.server.resources.pool.software.common
  (:require
   [leihs.inventory.server.resources.pool.models.common :refer [filter-map-by-spec]]
   [leihs.inventory.server.resources.pool.models.model.common-model-form :refer [replace-nil-with-empty-string]]))

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


