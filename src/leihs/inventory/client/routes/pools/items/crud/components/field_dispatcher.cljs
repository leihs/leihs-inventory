(ns leihs.inventory.client.routes.pools.items.crud.components.field-dispatcher
  (:require
   ["@/components/ui/popover" :refer [Popover PopoverContent PopoverTrigger]]
   ["lucide-react" :refer [Lock]]
   ["react-i18next" :refer [useTranslation]]
   [clojure.string :as str]
   [leihs.inventory.client.components.form.fields.attachments-field :refer [AttachmentsField]]
   [leihs.inventory.client.components.form.fields.autocomplete-field :refer [AutocompleteField]]
   [leihs.inventory.client.components.form.fields.calendar-field :refer [CalendarField]]
   [leihs.inventory.client.components.form.fields.checkbox-group-field :refer [CheckboxGroupField]]
   [leihs.inventory.client.components.form.fields.common-field :refer [CommonField]]
   [leihs.inventory.client.components.form.fields.composite-field :refer [CompositeField]]
   [leihs.inventory.client.components.form.fields.radio-group-field :refer [RadioGroupField]]
   [leihs.inventory.client.components.form.fields.select-field :refer [SelectField]]
   [leihs.inventory.client.lib.location-labels :as location-labels]
   [leihs.inventory.client.provider.visibility-provider :refer [use-field-visibility]]
   [leihs.inventory.client.routes.pools.items.crud.components.fields.items-field :refer [ItemsField]]
   [uix.core :refer [$ defui]]))

(def translations
  {:text
   {:select "pool.items.item.fields.autocomplete.select"
    :search "pool.items.item.fields.autocomplete.search"
    :empty "pool.items.item.fields.autocomplete.empty"}})

(defui FieldDispatcher [{:keys [form block]}]
  (let [[t] (useTranslation)
        translated-block (cond-> block
                           (and (:label block)
                                (not (str/starts-with? (:name block) "properties_")))
                           (update :label t))
        {:keys [is-visible
                values-dependency
                watched-dependency-value]} (use-field-visibility block)

        room-remap (if (= (:name block) "room_id")
                     location-labels/room-autocomplete-option
                     (fn [item] {:value (str (:id item))
                                 :label (:name item)}))

        label-inactive (fn [props]
                         (let [without-options (dissoc props :options)
                               text (t "pool.items.item.fields.inactive")
                               annotated (map #(if (and (boolean? (:is_active %))
                                                        (false? (:is_active %)))
                                                 (assoc % :label (str (:label %) " ( " text " )"))
                                                 %)
                                              (-> props :options))]
                           (assoc without-options :options annotated)))]

    (when is-visible
      ($ Popover
         ($ :div {:class-name "relative"}
            (when (:disabled (:props block))
              ($ PopoverTrigger {:asChild true
                                 :class-name "absolute right-0 z-10 top-1/2 m-1"}
                 ($ :button {:class-name "rounded text-muted-foreground bg-muted cursor-help"
                             :data-test-id (str (:name block) "-disabled-info")}
                    ($ Lock {:class-name "h-6 w-6 p-1"}))))

            (cond
              ;; Package-specific: item selection field
              (= (:name block) "item_ids")
              ($ ItemsField {:form form
                             :block translated-block})

              (-> block :component (= "attachments"))
              ($ AttachmentsField {:form form
                                   :label (:label translated-block)
                                   :name (:name translated-block)
                                   :props (:props translated-block)})

              ;; instant search via values-url
              (-> block :component (= "autocomplete-search"))
              ($ AutocompleteField {:form form
                                    :name (:name translated-block)
                                    :label (:label translated-block)
                                    :props (merge
                                            translations
                                            {:remap (fn [item] {:value (str (:id item))
                                                                :label (:name item)})}
                                            (:props translated-block))})

              (-> block :component (= "autocomplete"))
              ($ AutocompleteField {:form form
                                    :name (:name translated-block)
                                    :label (:label translated-block)
                                    :props (merge translations
                                                  (if values-dependency
                                                    (let [values-url (-> block :props :values-url)
                                                          dep (:field values-dependency)]
                                                      {:remap room-remap
                                                       :values-url (str values-url "?" dep "=" (.-value watched-dependency-value))})
                                                    (assoc (label-inactive (:props translated-block))
                                                           :remap room-remap)))})

              (-> block :component (= "checkbox"))
              ($ CheckboxGroupField {:form form
                                     :block translated-block})

              (-> block :component (= "composite"))
              ($ CompositeField {:form form
                                 :block translated-block})

              (-> block :component (= "radio-group"))
              ($ RadioGroupField {:form form
                                  :block translated-block})

              (-> block :component (= "select"))
              ($ SelectField {:form form
                              :block translated-block})

              (-> block :component (= "calendar"))
              ($ CalendarField {:form form
                                :block translated-block})

              ;; default case - renders a component from the component map
              :else
              ($ CommonField {:form form
                              :block translated-block})))

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
