(ns leihs.inventory.client.lib.dynamic-form)

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

;; Custom Fields Structure
;; =======================
;; Custom fields can be passed to fields->structure, fields->defaults,
;; and fields->schema to add frontend-defined fields alongside API fields.
;;
;; Custom field structure:
;; {:id "unique_field_id"           ;; REQUIRED: Unique field identifier (used as form key)
;;  :type "custom-type-name"        ;; REQUIRED: Type name (can be any string for custom fields)
;;  :component "component-name"     ;; REQUIRED: UI component to render (e.g., "input", "select", "textarea" ...)
;;  
;;  ;; Optional - Standard field attributes
;;  :group "Group Name"             ;; Group name (default: "Mandatory data")
;;  :group-after "Other Group"      ;; Insert this group after "Other Group" (only applies to new groups)
;;  :position 50                    ;; Position for sorting within group (lower = earlier)
;;  :label "Field Label"            ;; Display label for the field
;;  :required false                 ;; Is field required? (default: false)
;;  :default "default-value"        ;; Default value (type depends on field type)
;;  :description "Help text"        ;; Description/help text shown below field
;;  :placeholder "Placeholder..."   ;; Placeholder text for input fields
;;  
;;  ;; Optional - Custom field features
;;  :props {:type "number"          ;; Props passed directly to the component
;;          :min 0                  ;; These are merged with standard props
;;          :max 100
;;          :step 1}
;;  :validator (-> (z/string)       ;; Custom Zod validator (if not provided, defaults to string)
;;               (.min 1))
;;  
;;  ;; Optional - Advanced features (same as API fields)
;;  :visibility-dependency          ;; Show/hide based on another field's value
;;    {:field "other_field_id"
;;     :value "expected_value"}
;;  :values-dependency              ;; Field values depend on another field
;;    {:field "other_field_id"}
;;  :protected true                 ;; Make field read-only/disabled
;;  :exclude_from_submit true}      ;; Don't include in form submission
;;
;; Example - Custom number field:
;; [{:id "item_count"
;;   :type "number"
;;   :component "input"
;;   :group "Mandatory data"
;;   :position 3
;;   :label "Item Count"
;;   :required true
;;   :default 1
;;   :props {:type "number"
;;           :min 0
;;           :max 999999
;;           :step 1}
;;   :validator (-> (.. z -coerce (number))
;;                (.min 0 "Must be at least 0")
;;                (.max 999999 "Must be less than 1,000,000")
;;                (.int "Must be a whole number"))}]
;;
;; Example - Custom group positioned after existing group:
;; [{:id "custom_field"
;;   :type "text"
;;   :component "input"
;;   :group "My Custom Group"           ;; New group name
;;   :group-after "Mandatory data"      ;; Insert after "Mandatory data" group
;;   :position 10
;;   :label "Custom Field"}]
;;
;; Group Ordering:
;; - Groups are ordered by the minimum :position of fields within each group
;; - Use :group-after on any field in a custom group to position that group after a specific existing group
;; - If :group-after references a non-existent group, the group will be placed at the end
;; - Fields within each group are sorted by their :position value
;;
;; Usage:
;; (def custom-fields [{...}])
;; (def all-fields (concat (:fields api-response) custom-fields))
;; (dynamic-form/fields->structure all-fields)
;; (dynamic-form/fields->defaults all-fields)
;; (dynamic-validation/fields->schema all-fields)

(defn- field-type->component [field]
  ;; If field has explicit component, use it (for custom fields)
  (or (:component field)
      ;; Otherwise use standard type mapping
      (case (:type field)
        "text" "input"
        "textarea" "textarea"
        "date" "calendar"
        "select" "select"
        "radio" "radio-group"
        "checkbox" "checkbox"
        "attachment" "attachments"
        "autocomplete-search" "autocomplete-search"
        "autocomplete" "autocomplete"
        nil)))

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
        component (field-type->component field)
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
                    (assoc :values-url (:values_url field))

                    ;; Merge custom props if provided
                    (:props field) (merge (:props field)))

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
          values-dep (assoc :values-dependency values-dep)
          (:protected field) (assoc :disabled-reason :protected))))))

(defn- group-fields-by-group [fields]
  (reduce (fn [acc field]
            (let [group-name (or (:group field) "Mandatory data")]
              (update acc group-name (fnil conj []) field)))
          {}
          fields))

(defn- calculate-group-metadata
  "Calculates metadata for each group including min position and group-after directive.
   Returns a map of {group-name {:fields [...] :min-position N :group-after \"name\"}}"
  [grouped]
  (reduce (fn [acc [group-name group-fields]]
            (let [min-pos (apply min (map #(or (:position %) 999999) group-fields))
                  group-after (some :group-after group-fields)]
              (assoc acc group-name {:fields group-fields
                                     :min-position min-pos
                                     :group-after group-after})))
          {}
          grouped))

(defn- order-groups
  "Orders groups based on min position and :group-after directives.
   Returns ordered vector of [group-name group-metadata]."
  [group-metadata]
  (let [;; First, sort all groups by their min position
        sorted-by-position (sort-by (fn [[_ meta]] (:min-position meta))
                                    group-metadata)

        ;; Build a map for quick lookup of group positions
        position-map (into {} (map-indexed (fn [idx [name _]] [name idx])
                                           sorted-by-position))

        ;; Separate groups with :group-after from those without
        [with-after without-after] (reduce (fn [[wa woa] [name meta :as entry]]
                                             (if (:group-after meta)
                                               [(conj wa entry) woa]
                                               [wa (conj woa entry)]))
                                           [[] []]
                                           sorted-by-position)

        ;; Process groups with :group-after directives
        groups-to-insert (reduce (fn [acc [name meta]]
                                   (let [target-group (:group-after meta)]
                                     (if (contains? position-map target-group)
                                       (conj acc {:name name
                                                  :meta meta
                                                  :insert-after target-group})
                                       (do
                                         (js/console.warn
                                          (str "Group '" name "' has :group-after '"
                                               target-group "' but that group doesn't exist. "
                                               "Placing at end."))
                                         acc))))
                                 []
                                 with-after)

        ;; Start with base groups (those without :group-after)
        base-groups (vec without-after)

        ;; Insert groups at their specified positions
        final-groups (reduce (fn [groups {:keys [name meta insert-after]}]
                               (let [;; Find position of target group in current groups vector
                                     target-idx (some (fn [[idx [g-name _]]]
                                                        (when (= g-name insert-after) idx))
                                                      (map-indexed vector groups))]
                                 (if target-idx
                                   ;; Insert after the target group
                                   (let [insert-pos (inc target-idx)]
                                     (vec (concat (take insert-pos groups)
                                                  [[name meta]]
                                                  (drop insert-pos groups))))
                                   ;; Target not found, append at end
                                   (conj groups [name meta]))))
                             base-groups
                             groups-to-insert)]
    final-groups))

(defn fields->structure [fields]
  (let [;; Filter only implemented field types or fields with custom component
        implemented-fields (filter #(or (implemented-field-types (:type %))
                                        (:component %))
                                   fields)
        ;; Group fields by group name
        grouped (group-fields-by-group implemented-fields)
        ;; Calculate metadata (min position, group-after) for each group
        group-metadata (calculate-group-metadata grouped)
        ;; Order groups based on position and :group-after directives
        ordered-groups (order-groups group-metadata)]

    ;; Map ordered groups to structure format
    (mapv (fn [[group-name meta]]
            {:title (str "fields." group-name ".title")
             :blocks (->> (:fields meta)
                          (sort-by :position)
                          (map transform-field)
                          (filter some?)
                          vec)})
          ordered-groups)))

(defn fields->defaults [fields]
  (let [;; Filter only implemented field types or fields with custom component
        implemented-fields (filter #(or (implemented-field-types (:type %))
                                        (:component %))
                                   fields)]
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
                                    ;; Default for custom/unknown types
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

                                      ;; Default for custom/unknown types - use as-is
                                      default-val))]
                (assoc acc field-id converted-val)))
            {}
            implemented-fields)))

(defn patch
  "Patches a specific field in the structure.
   
   Args:
     structure - The form structure (vector of sections)
     field-name - The name of the field to update (string)
     updates - Map of updates to apply. Can include:
               :props - Props to merge into the field's :props
               :disabled-reason - Reason for disabling (keyword)
               Any other block-level keys (e.g., :visibility-dependency, :values-dependency)
   
   Returns:
     Patched structure with the field modified.
     Returns unchanged structure if field-name not found.
   
   Example:
     (patch structure \"inventory_code\" 
                   {:props {:disabled true}
                    :disabled-reason :multiple-items})
     (patch structure \"model_id\" 
                   {:props {:disabled false}
                    :disabled-reason nil})"
  [structure field-name updates]
  (mapv (fn [section]
          (update section :blocks
                  (fn [blocks]
                    (mapv (fn [block]
                            (if (= (:name block) field-name)
                              ;; Separate :props updates from block-level updates
                              (let [props-updates (:props updates)
                                    block-updates (dissoc updates :props)]
                                (cond-> block
                                  ;; Merge props if provided
                                  props-updates (update :props merge props-updates)
                                  ;; Merge block-level updates
                                  (seq block-updates) (merge block-updates)))
                              block))
                          blocks))))
        structure))
