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

(defn- convert-string-booleans [obj]
  (let [entries (js/Object.entries obj)]
    (reduce (fn [result [k v]]
              (aset result k
                    (cond
                      (= v "true") true
                      (= v "false") false
                      :else v))
              result)
            (js-obj)
            entries)))

(defn fields-to-zod-schema [fields-response]
  (let [fields (-> fields-response :fields)
        schema-obj (reduce (fn [acc field]
                             (let [field-id (:id field)
                                   validator (field->zod-validator field)]
                               (assoc acc field-id validator)))
                           {}
                           fields)
        excluded-fields (set (map :id (filter :exclude_from_submit fields)))
        base-schema (z/object (clj->js schema-obj))

        ;; 🧩 Add conditional cross-field refinement:
        refined-schema (.refine
                        base-schema
                        (fn [data]
                          (let [room-id (aget data "room_id")
                                building-id (aget data "building_id")]
                            ;; ✅ Rule: If room_id has a value, building_id must not be nullish
                            (js/console.debug "Refinement check:" data room-id building-id)
                            (or (not room-id)
                                (some? building-id))))
                        (clj->js {:message "Building must be selected before choosing a room"
                                  :path ["room_id"]}))]

    (.transform refined-schema
                (fn [data]
                  (let [converted (convert-string-booleans data)]
                    (if (seq excluded-fields)
                      (reduce (fn [obj field-id]
                                (js-delete obj (name field-id))
                                obj)
                              converted
                              excluded-fields)
                      converted))))))
