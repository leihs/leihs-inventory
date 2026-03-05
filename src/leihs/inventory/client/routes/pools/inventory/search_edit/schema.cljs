(ns leihs.inventory.client.routes.pools.inventory.search-edit.schema
  (:require
   ["date-fns" :refer [format]]
   ["zod" :as z]
   [leihs.inventory.client.lib.utils :refer [cj jc]]))

;; Zod validation schema for advanced search filters
;;
;; This schema validates and transforms the complex form structure from React Hook Form
;; into a simplified structure suitable for the query builder.
;;
;; INPUT (from React Hook Form):
;; {:$or [{:id "uuid-1"
;;         :$and [{:name "inventory_code"
;;                 :component "input"
;;                 :props {:type "text" :autoComplete "off" :required true}
;;                 :id "condition-uuid-1"
;;                 :value "ITZ"
;;                 :operator "$ilike"
;;                 :allowed-operators ["$ilike"]}]}]}
;;
;; OUTPUT (after transformation):
;; {:$or [{:$and [{:inventory_code {:$ilike" "ITZ"}}]}]}

;; Schema for a single AND condition
;; Transforms from full condition object to simplified structure
(def and-condition-schema
  (-> (z/object
       (cj {:name (z/string) ;; field name (e.g., "inventory_code", "properties_reference")
            :value (z/any) ;; the actual value entered by user
            :operator (z/literal #js ["$eq" "$ilike" "$gte" "$lte"])}))
      (.transform
       (fn [condition]
         ;; Transform to query structure -> {:field_name {:$operator value}}
         (cj {(keyword (.-name condition))
              (let [val (.-value condition)
                    ;; Format dates to YYYY-MM-DD if the value is a Date object
                    formatted-val (if (instance? js/Date val)
                                    (format val "yyyy-MM-dd")
                                    val)
                    ;; Coerce "true"/"false" strings to booleans
                    coerced-val (cond
                                  (= formatted-val "true")
                                  true
                                  (= formatted-val "false")
                                  false
                                  (= (.-name condition) "price")
                                  (if (= (.-value condition) "")
                                    0
                                    (js/parseFloat formatted-val))
                                  :else formatted-val)]
                {(keyword (.-operator condition))
                 (if (and (object? coerced-val) (some? (.-value coerced-val)))
                   (.-value coerced-val)
                   coerced-val)})})))))

;; Schema for an OR group
;; Transforms from group with id to just the $and array
(def or-group-schema
  (-> (z/object
       (cj {:id (z/string) ;; group identifier
            :$and (z/array and-condition-schema)})) ;; array of AND conditions
      (.transform
       (fn [group]
         ;; Keep only the transformed $and array
         (cj {:$and ^js (.-$and group)})))))

;; Main schema for the search edit form
;; Validates and transforms the entire form structure
(def search-edit-schema
  (-> (z/object
       (cj {:$or (z/array or-group-schema)})) ;; array of OR groups
      (.transform
       (fn [data]
         ;; Keep the $or structure with transformed nested data
         (cj {:$or ^js (.-$or data)})))))
