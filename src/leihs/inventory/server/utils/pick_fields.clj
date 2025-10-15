(ns leihs.inventory.server.utils.pick-fields
  (:require
   [clojure.string :as str]
   [ring.util.codec :as codec]
   [taoensso.timbre :refer [debug]]))

(defn parse-fields
  "Parses a URL-encoded query parameter like 'fields=field1%2Cfield2%2Cfield3' 
   and returns a set of keyword fields."
  [fields-param]
  (when (and fields-param (not (str/blank? fields-param)))
    (->> (codec/url-decode fields-param)
         (#(str/split % #","))
         (map keyword)
         set)))

(defn- extract-valid-fields
  "Extract valid field names from a schema definition.
   Accepts either a vector of keywords/strings, a map/schema object, or Schema record."
  [schema]
  (cond
    ;; Vector of field names
    (sequential? schema)
    (set (map keyword schema))
    
    ;; Map with keys as field names (including Schema maps)
    (map? schema)
    (->> (keys schema)
         (map #(if (keyword? %) % (when (map? %) (:k %))))
         (filter some?)
         (map keyword)
         set)
    
    ;; Default: empty set
    :else #{}))

(defn pick-fields
  "Filters data to only include requested fields, validating against schema.
   
   Args:
     data: vector of maps, or a map containing :data (with optional :pagination etc.)
     fields-param: URL-encoded comma-separated field list string
     valid-fields: schema defining valid fields (vector of keywords, or map with field keys)
   
   Returns: same structure as data but with filtered fields."
  [data fields-param valid-fields]
  (if-let [requested-fields (parse-fields fields-param)]
    (let [valid-field-set (extract-valid-fields valid-fields)
          invalid-fields (remove valid-field-set requested-fields)]
      
      (when (seq invalid-fields)
        (throw (IllegalArgumentException.
                (str "Invalid fields requested: "
                     (str/join ", " (map name invalid-fields))))))
      
      (debug "pick-fields: requested" requested-fields "valid" valid-field-set)
      
      (let [pick (fn [m] (select-keys m requested-fields))]
        (cond
          ;; Wrapped form: {:data [...] :pagination ...}
          (and (map? data) (vector? (:data data)))
          (update data :data #(mapv pick %))
          
          ;; Direct vector of maps
          (sequential? data)
          (mapv pick data)
          
          ;; Single map
          (map? data)
          (pick data)
          
          ;; Unexpected: return unchanged
          :else data)))
    
    ;; No fields specified: return data unchanged
    data))
