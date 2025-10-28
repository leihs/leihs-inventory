(ns leihs.inventory.client.lib.fields-to-zod
  (:require
   ["zod" :as z]))

(defn- field->zod-validator [field]
  (let [field-type (:type field)
        is-required (:required field)
        base-validator (case field-type
                         "text" (z/string)
                         "textarea" (z/string)
                         "date" (z/date)
                         "select" (z/string)
                         "radio" (z/string)
                         "checkbox" (z/boolean)
                         "autocomplete-search" (z/string)
                         "autocomplete" (z/string)
                         "attachment" (z/array (z/any))
                         (z/string))]

    (if is-required
      base-validator
      (.. z (optional base-validator)))))

(defn fields-to-zod-schema [fields-response]
  (let [fields (-> fields-response :fields)
        schema-obj (reduce (fn [acc field]
                             (let [field-id (keyword (:id field))
                                   validator (field->zod-validator field)]
                               (assoc acc field-id validator)))
                           {}
                           fields)]
    (z/object (clj->js schema-obj))))
