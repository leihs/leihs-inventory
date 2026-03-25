(ns leihs.inventory.client.routes.pools.items.crud.components.field-dispatcher
  (:require
   ["@/components/ui/popover" :refer [Popover PopoverContent PopoverTrigger]]
   ["lucide-react" :refer [Lock]]
   ["react-hook-form" :refer [useWatch]]
   ["react-i18next" :refer [useTranslation]]
   [leihs.inventory.client.components.form.fields.attachments-field :refer [AttachmentsField]]
   [leihs.inventory.client.components.form.fields.autocomplete-field :refer [AutocompleteField]]
   [leihs.inventory.client.components.form.fields.calendar-field :refer [CalendarField]]
   [leihs.inventory.client.components.form.fields.common-field :refer [CommonField]]
   [leihs.inventory.client.components.form.fields.radio-group-field :refer [RadioGroupField]]
   [leihs.inventory.client.components.form.fields.select-field :refer [SelectField]]
   [uix.core :refer [$ defui]]))

(def translations
  {:text
   {:select "pool.items.item.fields.autocomplete.select"
    :search "pool.items.item.fields.autocomplete.search"
    :empty "pool.items.item.fields.autocomplete.empty"}})

(defn- has-value?
  "Check if a value is considered 'truthy' for dependency purposes.
   Returns false for: nil, empty string, empty array, empty object, false"
  [val]
  (cond
    (nil? val) false
    (boolean? val) val
    (string? val) (not= val "")
    (array? val) (pos? (.-length val))
    (object? val) (if (js/Object.hasOwn val "value")
                    ;; For objects like {:value "..." :label "..."}, check the value property
                    (has-value? (.-value val))
                    ;; For plain objects without value property, check if they have keys
                    (pos? (count (js/Object.keys val))))
    :else true))

(defui FieldDispatcher [{:keys [form block]}]
  (let [[t] (useTranslation)
        control (.-control form)
        visibility (:visibility-dependency block)
        values-dep (:values-dependency block)

        ;; Always call useWatch (hooks must be called unconditionally)
        watched-visibility (useWatch #js {:control control
                                          :name (:field visibility)})
        watched-dependency (useWatch #js {:control control
                                          :name (:field values-dep)})

        ;; Check visibility
        is-visible (if visibility
                     (= (str watched-visibility) (str (:value visibility)))
                     true)

        ;; Check if field should show based on values dependency
        has-dependency-value (if values-dep
                               (has-value? watched-dependency)
                               true)

        label-inactive (fn [props]
                         (let [without-options (dissoc props :options)
                               text (t "pool.items.item.fields.inactive")
                               annotated (map #(if (and (boolean? (:is_active %))
                                                        (false? (:is_active %)))
                                                 (assoc % :label (str (:label %) " ( " text " )"))
                                                 %)
                                              (-> props :options))]
                           (assoc without-options :options annotated)))]

    (when (and is-visible has-dependency-value)
      ($ Popover
         ($ :div {:class-name "relative"}
            (when (:disabled (:props block))
              ($ PopoverTrigger {:asChild true
                                 :class-name "absolute right-0 z-10 top-1/2 m-1"}
                 ($ :button {:class-name "rounded text-muted-foreground bg-muted cursor-help"
                             :data-test-id (str (:name block) "-disabled-info")}
                    ($ Lock {:class-name "h-6 w-6 p-1"}))))

            (cond
              (-> block :component (= "attachments"))
              ($ AttachmentsField {:form form
                                   :label (t (:label block))
                                   :name (:name block)
                                   :props (:props block)})

              ;; instant search via values-url
              (-> block :component (= "autocomplete-search"))
              ($ AutocompleteField {:form form
                                    :name (:name block)
                                    :label (:label block)
                                    :props (merge
                                            translations
                                            {:remap (fn [item] {:value (str (:id item))
                                                                :label (:name item)})}
                                            (:props block))})

              (-> block :component (= "autocomplete"))
              ($ AutocompleteField {:form form
                                    :name (:name block)
                                    :label (:label block)
                                    :props (merge translations
                                                  (if values-dep
                                                    (let [values-url (-> block :props :values-url)
                                                          dep (:field values-dep)]
                                                      {:remap (fn [item] {:value (str (:id item))
                                                                          :label (str (:name item))})
                                                       :values-url (str values-url "?" dep "=" (.-value watched-dependency))})
                                                    (label-inactive (:props block))))})

              (-> block :component (= "radio-group"))
              ($ RadioGroupField {:form form
                                  :block block})

              (-> block :component (= "select"))
              ($ SelectField {:form form
                              :block block})

              (-> block :component (= "calendar"))
              ($ CalendarField {:form form
                                :block block})

              ;; default case - renders a component from the component map
              :else
              ($ CommonField {:form form
                              :block block})))

         (when (:disabled (:props block))
           ($ PopoverContent {:side "top"
                              :class-name "w-[150px] text-sm"}
              (case (:disabled-reason block)
                :protected (t "pool.items.item.fields.disabled.protected")
                :model-selected (t "pool.items.item.fields.disabled.model-selected")
                :multiple-items (t "pool.items.item.fields.disabled.multiple-items")
                :owner-locked (t "pool.items.item.fields.disabled.owner-locked")
                ;; Fallback for fields disabled without a reason
                (t "pool.items.item.fields.disabled.generic"))))))))
