(ns leihs.inventory.client.routes.pools.packages.crud.components.field-dispatcher
  (:require
   ["@/components/ui/popover" :refer [Popover PopoverContent PopoverTrigger]]
   ["lucide-react" :refer [Lock]]
   ["react-i18next" :refer [useTranslation]]
   [leihs.inventory.client.components.form.fields.attachments-field :refer [AttachmentsField]]
   [leihs.inventory.client.components.form.fields.autocomplete-field :refer [AutocompleteField]]
   [leihs.inventory.client.components.form.fields.calendar-field :refer [CalendarField]]
   [leihs.inventory.client.components.form.fields.common-field :refer [CommonField]]
   [leihs.inventory.client.components.form.fields.radio-group-field :refer [RadioGroupField]]
   [leihs.inventory.client.components.form.fields.select-field :refer [SelectField]]
   [leihs.inventory.client.provider.visibility-provider :refer [use-field-visibility]]
   [leihs.inventory.client.routes.pools.packages.crud.components.fields.items-field :refer [ItemsField]]
   [uix.core :as uix :refer [$ defui]]))

(def translations
  {:text
   {:select "pool.packages.package.fields.autocomplete.select"
    :search "pool.packages.package.fields.autocomplete.search"
    :empty "pool.packages.package.fields.autocomplete.empty"}})

(defui FieldDispatcher [{:keys [form block]}]
  (let [[t] (useTranslation)
        {:keys [is-visible
                values-dependency
                watched-dependency-value]} (use-field-visibility block)]

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
              (-> block :component (= "items"))
              ($ ItemsField {:form form
                             :block block})

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
                                                  (if values-dependency
                                                    (let [values-url (-> block :props :values-url)
                                                          dep (:field values-dependency)]
                                                      {:remap (fn [item] {:value (str (:id item))
                                                                          :label (:name item)})
                                                       :values-url (str values-url "?" dep "=" (.-value watched-dependency-value))})
                                                    (:props block)))})

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
                :protected (t "pool.packages.package.fields.disabled.protected")
                ;; Fallback for fields disabled without a reason
                (t "pool.packages.package.fields.disabled.generic"))))))))
