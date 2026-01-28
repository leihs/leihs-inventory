(ns leihs.inventory.client.lib.dynamic-validation
  (:require
   ["zod" :as z]
   [clojure.set]))

(defn- field->zod-validator [field]
  (let [field-type (:type field)
        is-required (:required field)
        is-protected (:protected field)
        ;; Fields with dependencies should be treated as optional in base validation
        ;; so that we can add custom refinements later
        has-dependency (or (:visibility_dependency_field_id field)
                           (:values_dependency_field_id field))
        treat-as-optional (and is-required has-dependency)

        base-validator (case field-type
                         "text"
                         (let [base-string (if (and is-required (not treat-as-optional))
                                             (-> (z/string)
                                                 (.min 1))
                                             (z/string))]
                           (.transform base-string
                                       (fn [val]
                                         (when val
                                           (if (re-matches #"^\d+[,]\d+$" val)
                                             (.replace val "," ".")
                                             val)))))

                         "textarea"
                         (if (and is-required (not treat-as-optional))
                           (-> (z/string)
                               (.min 1))
                           (z/string))

                         "date"
                         (-> (.. z -coerce (date))
                             ;; transform to "YYYY-MM-DD" format
                             (.transform (fn [date]
                                           (when date
                                             (let [year (.getFullYear date)
                                                   month (-> (.getMonth date) inc (str) (.padStart 2 "0"))
                                                   day (-> (.getDate date) (str) (.padStart 2 "0"))]
                                               (str year "-" month "-" day))))))

                         "select"
                         (if (and is-required (not treat-as-optional))
                           (-> (z/string)
                               (.min 1))
                           (z/string))

                         "radio"
                         (if (and is-required (not treat-as-optional))
                           (-> (z/string)
                               (.min 1 "Please select an option"))
                           (z/string))

                         ;; Both autocomplete types validate as object, transform to string
                         "autocomplete-search"
                         (if (and is-required (not treat-as-optional))
                           (-> (z/object (clj->js {:value (-> (z/string)
                                                              (.min 1))
                                                   :label (z/nullish (z/string))}))
                               (.transform (fn [obj] (.-value obj))))
                           (-> (z/object (clj->js {:value (z/nullish (z/string))
                                                   :label (z/nullish (z/string))}))
                               (.transform (fn [obj] (.-value obj)))))

                         "autocomplete"
                         (if (and is-required (not treat-as-optional))
                           (-> (z/object (clj->js {:value (-> (z/string)
                                                              (.min 1))
                                                   :label (z/nullish (z/string))}))
                               (.transform (fn [obj] (.-value obj))))
                           (-> (z/object (clj->js {:value (z/nullish (z/string))
                                                   :label (z/nullish (z/string))}))
                               (.transform (fn [obj] (.-value obj)))))

                         "attachment"
                         (if (and is-required (not treat-as-optional))
                           (-> (z/array (z/any))
                               (.min 1 "At least one attachment is required"))
                           (z/array (z/any)))

                         ;; Default for custom/unknown types
                         (if-let [custom-validator (:validator field)]
                           custom-validator
                           ;; Fall back to string validation
                           (if (and is-required (not treat-as-optional))
                             (-> (z/string) (.min 1))
                             (z/string))))]

    (if (or (not is-required) (not is-protected) treat-as-optional)
      (z/nullish (z/optional base-validator))
      base-validator)))

(defn- convert-string-booleans [obj]
  (let [entries (js/Object.entries obj)]
    (reduce (fn [result [k v]]
              (aset result k
                    (cond
                      (= v "true") true
                      (= v "false") false
                      (= v "") nil
                      :else v))
              result)
            (js-obj)
            entries)))

(defn fields->schema [fields]
  (let [;; Filter out protected fields from schema
        non-protected-fields (filter #(not (:protected %)) fields)
        schema-obj (reduce (fn [acc field]
                             (let [field-id (:id field)
                                   validator (field->zod-validator field)]
                               (assoc acc field-id validator)))
                           {}
                           non-protected-fields)
        excluded-fields (set (map :id (filter :exclude_from_submit fields)))
        protected-fields (set (map :id (filter :protected fields)))
        base-schema (z/object (clj->js schema-obj))

        ;; Add conditional cross-field refinements:
        refined-schema (.superRefine
                        base-schema
                        (fn [data ^js ctx]
                          ;; Check all fields with dependencies (excluding protected)
                          (doseq [field non-protected-fields]
                            (let [field-id (:id field)
                                  field-value (aget data field-id)
                                  is-required (:required field)
                                  values-dep-id (:values_dependency_field_id field)
                                  visibility-dep-id (:visibility_dependency_field_id field)
                                  visibility-dep-value (:visibility_dependency_value field)]

                              ;; Only validate required fields with dependencies
                              (when is-required
                                ;; Case 1: values_dependency - field should only validate if dependency field has a value
                                (when values-dep-id
                                  (let [dep-value (aget data values-dep-id)]
                                    (when (and (some? dep-value)
                                               (not (some? field-value)))
                                      (.addIssue ctx (clj->js {:code "custom"
                                                               :message (str (:label field) " must be selected")
                                                               :path [field-id]})))))

                                ;; Case 2: visibility_dependency - field should only validate if dependency field matches specific value
                                (when visibility-dep-id
                                  (let [dep-field-value (aget data visibility-dep-id)
                                        matches (or (= dep-field-value visibility-dep-value)
                                                    (= (str dep-field-value) visibility-dep-value)
                                                    (and (= visibility-dep-value "true") (= dep-field-value true))
                                                    (and (= visibility-dep-value "false") (= dep-field-value false)))

                                        ;; Check if field is empty (null, undefined, or empty string)
                                        is-empty (or (nil? field-value)
                                                     (= field-value "")
                                                     (= field-value js/undefined))]

                                    ;; Handle both string and boolean comparisons
                                    (when (and matches is-empty)
                                      (.addIssue ctx (clj->js {:code "custom"
                                                               :message (str (:label field) " is required")
                                                               :path [field-id]})))))))
                            data)))]

    ;; convert string "true"/"false" to boolean, and remove excluded and protected fields
    (.transform refined-schema
                (fn [data]
                  (let [converted (convert-string-booleans data)
                        fields-to-remove (clojure.set/union excluded-fields protected-fields)]
                    (if (seq fields-to-remove)
                      (reduce (fn [obj field-id]
                                (js-delete obj (name field-id))
                                obj)
                              converted
                              fields-to-remove)
                      converted))))))
