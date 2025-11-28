(ns leihs.inventory.client.lib.fields-to-form)

(def implemented-field-types
  #{"text"
    "textarea"
    "date"
    "select"
    "radio"
    "checkbox"
    "attachment"
    "autocomplete-search"
    "autocomplete"})

(defn- field-type->component [field-type]
  (case field-type
    "text" "input"
    "textarea" "textarea"
    "date" "calendar"
    "select" "select"
    "radio" "radio-group"
    "checkbox" "checkbox"
    "attachment" "attachments"
    "autocomplete-search" "autocomplete-search"
    "autocomplete" "autocomplete"
    nil))

(defn- transform-field-values [values field-type]
  (when values
    (mapv (fn [v]
            (if (map? v)
              (cond
                (contains? v :value)
                {:value (str (:value v))
                 :label (:label v)
                 :is_active (:is_active v)}

                :else v)
              {:value (str v) :label (str v)}))
          values)))

(defn- transform-field [field]
  (let [id (:id field)
        field-type (:type field)
        component (field-type->component field-type)
        group-name (or (:group field) "Mandatory data")]
    (when component
      (let [base-block {:name (:id field)
                        :label (str "fields." group-name "." (:id field))
                        :component component}
            ;; For autocomplete-search, construct proper resource URL with search param
            search-resource (when (= field-type "autocomplete-search")
                              (let [base-url (:values_url field)
                                    search-attr (:search_attr field "search_term")
                                   ;; Check if URL already has query params
                                    separator (if (clojure.string/includes? base-url "?") "&" "?")]
                                (str base-url separator search-attr "=")))

            props (cond-> {}
                    (= field-type "text") (assoc :type "text"
                                                 :autoComplete "off")
                    (= field-type "date") (assoc :mode "single")
                    (= field-type "attachment") (assoc :multiple true)
                    (= field-type "autocomplete-search") (assoc :values-url search-resource
                                                                :instant true)

                    (contains? field :values) (assoc :options (transform-field-values (:values field) field-type))
                    (contains? field :placeholder) (assoc :placeholder (:placeholder field))
                    (contains? field :required) (assoc :required (:required field))
                    (contains? field :protected) (assoc :disabled (:protected field))

                    ;; For autocomplete with values_url, pass the URL
                    (and (= field-type "autocomplete") (:values_url field))
                    (assoc :values-url (:values_url field)))

            ;; Add visibility dependency if present
            visibility-dep (when (and (:visibility_dependency_field_id field)
                                      (:visibility_dependency_value field))
                             {:field (:visibility_dependency_field_id field)
                              :value (:visibility_dependency_value field)})

            ;; Add values dependency if present (e.g., room depends on building)
            values-dep (when (:values_dependency_field_id field)
                         {:field (:values_dependency_field_id field)})]

        (cond-> base-block
          (seq props) (assoc :props props)
          (:description field) (assoc :description (:description field))
          visibility-dep (assoc :visibility-dependency visibility-dep)
          values-dep (assoc :values-dependency values-dep))))))

(defn- group-fields-by-group [fields]
  (reduce (fn [acc field]
            (let [group-name (or (:group field) "Mandatory data")]
              (update acc group-name (fnil conj []) field)))
          {}
          fields))

(defn transform-fields-to-structure [fields-response]
  (let [fields (-> fields-response :fields)
        ;; Filter only implemented field types
        implemented-fields (filter #(implemented-field-types (:type %)) fields)
        grouped (group-fields-by-group implemented-fields)]

    (mapv (fn [[group-name group-fields]]
            {:title (str "fields." group-name ".title")
             :blocks (->> group-fields
                          (sort-by :position)
                          (map transform-field)
                          (filter some?)
                          vec)})
          grouped)))

(defn extract-default-values [fields-response]
  (let [fields (-> fields-response :fields)
        implemented-fields (filter #(implemented-field-types (:type %)) fields)]
    (reduce (fn [acc field]
              (let [field-id (keyword (:id field))
                    field-type (:type field)
                    has-default? (contains? field :default)
                    default-val (if has-default?
                                  (if (boolean? (:default field))
                                    (str (:default field))
                                    (:default field))
                                  ;; Set type-specific defaults when no default provided
                                  (case field-type
                                    "text" ""
                                    "textarea" ""
                                    "select" nil
                                    "date" nil
                                    "radio" false
                                    "checkbox" false
                                    "attachment" []
                                    "autocomplete-search" {:value nil
                                                           :label nil}
                                    "autocomplete" {:value nil
                                                    :label nil}
                                    nil))

                    ;; Convert default value based on field type
                    converted-val (when (or (some? default-val)
                                            (contains? #{"text" "textarea"} field-type))
                                    (case field-type
                                      "text"
                                      (if (nil? default-val)
                                        ""
                                        (str default-val))

                                      "textarea"
                                      (if (nil? default-val)
                                        ""
                                        default-val)

                                      "date"
                                      (if (= default-val "today")
                                        (js/Date.)
                                        default-val)

                                      "attachment"
                                      (if (vector? default-val) default-val [])

                                      default-val))]
                (assoc acc field-id converted-val)))
            {}
            implemented-fields)))
