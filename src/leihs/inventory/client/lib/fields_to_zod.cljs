(ns leihs.inventory.client.lib.fields-to-zod
  (:require
   ["zod" :as z]))

(defn- field->zod-validator [field]
  (let [field-type (:type field)
        is-required (:required field)
        ;; Fields with dependencies should be treated as optional in base validation
        has-dependency (or (:visibility_dependency_field_id field)
                           (:values_dependency_field_id field))
        treat-as-optional (and is-required has-dependency)

        base-validator (case field-type
                         "text"
                         (if (and is-required (not treat-as-optional))
                           (-> (z/string)
                               (.min 1))
                           (z/string))

                         "textarea"
                         (if (and is-required (not treat-as-optional))
                           (-> (z/string)
                               (.min 1))
                           (z/string))

                         "date"
                         (z/date)

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

                         (z/string))]

    (if (or (not is-required) treat-as-optional)
      (z/nullish (z/optional base-validator))
      base-validator)))

(defn fields-to-zod-schema [fields-response]
  (let [fields (-> fields-response :fields)
        schema-obj (reduce (fn [acc field]
                             (let [field-id (:id field)
                                   validator (field->zod-validator field)]
                               (assoc acc field-id validator)))
                           {}
                           fields)]
    (z/object (clj->js schema-obj))))
