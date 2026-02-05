(ns leihs.inventory.client.routes.pools.packages.crud.components.fields
  (:require
   ["@/components/ui/popover" :refer [Popover PopoverContent PopoverTrigger]]
   ["@@/button" :refer [Button]]
   ["@@/calendar" :refer [Calendar]]
   ["@@/dropzone" :refer [Dropzone]]
   ["@@/form" :refer [FormControl FormDescription FormField FormItem FormLabel
                      FormMessage]]
   ["@@/input" :refer [Input]]
   ["@@/radio-group" :refer [RadioGroup RadioGroupItem]]
   ["@@/select" :refer [Select SelectContent SelectItem SelectTrigger
                        SelectValue]]
   ["@@/textarea" :refer [Textarea]]
   ["@@/tooltip" :refer [Tooltip TooltipContent TooltipTrigger]]
   ["date-fns" :refer [format]]
   ["lucide-react" :refer [CalendarIcon]]
   ["react-hook-form" :refer [useWatch]]
   ["react-i18next" :refer [useTranslation]]
   [leihs.inventory.client.components.form.attachments :refer [Attachments]]
   [leihs.inventory.client.components.form.autocomplete :refer [Autocomplete]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]

   [uix.core :as uix :refer [$ defui]]))

(def fields-map
  {"input" Input
   "dropzone" Dropzone
   "textarea" Textarea})

(def translations
  {:text
   {:select "pool.packages.package.fields.autocomplete.select"
    :search "pool.packages.package.fields.autocomplete.search"
    :empty "pool.packages.package.fields.autocomplete.empty"}})

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

(defui field [{:keys [control form block]}]
  (let [[t] (useTranslation)
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
                               true)]

    (when (and is-visible has-dependency-value)
      ($ Tooltip
         ($ TooltipTrigger {:asChild true}
            ($ :div
               (case (:component block)
                 "attachments"
                 ($ Attachments {:form form
                                 :label (t (:label block))
                                 :name (:name block)
                                 :props (:props block)})

                 ;; instant search via values-url
                 "autocomplete-search"
                 ($ Autocomplete {:form form
                                  :name (:name block)
                                  :label (:label block)
                                  :props (merge
                                          translations
                                          {:remap (fn [item] {:value (str (:id item))
                                                              :label (:name item)})}
                                          (:props block))})

                 "autocomplete"
                 ($ Autocomplete {:form form
                                  :name (:name block)
                                  :label (:label block)
                                  :props (merge translations
                                                (if values-dep
                                                  (let [values-url (-> block :props :values-url)
                                                        dep (:field values-dep)]
                                                    {:remap (fn [item] {:value (str (:id item))
                                                                        :label (:name item)})
                                                     :values-url (str values-url "?" dep "=" (.-value watched-dependency))})
                                                  (:props block)))})

                 ;; Radiogroup field
                 "radio-group"
                 ($ FormField {:control (cj control)
                               :name (:name block)
                               :render #($ FormItem {:class-name "mt-6"
                                                     :title (when (:disabled (:props block))
                                                              "This field is disabled/protected.")}
                                           ($ FormLabel (t (:label block))
                                              (when (-> block :props :required) " *"))

                                           ($ FormControl
                                              ($ RadioGroup {:onValueChange (aget % "field" "onChange")
                                                             :defaultValue (aget % "field" "value")
                                                             :class-name "flex space-x-1"
                                                             :name (:name block)}

                                                 (for [option (:options (:props block))]
                                                   ($ FormItem {:key (:value option)
                                                                :class-name "flex items-center space-x-2 space-y-0"}
                                                      ($ FormControl
                                                         ($ RadioGroupItem {:data-test-id (str (:name block) "-" (:value option))
                                                                            :disabled (:disabled (:props block))
                                                                            :value (:value option)}))
                                                      ($ FormLabel {:class-name "font-normal"}
                                                         (:label option)))))))})

                 ;; Select field
                 "select"
                 ($ FormField {:control (cj control)
                               :name (:name block)
                               :render #($ FormItem {:class-name "mt-6"}
                                           ($ FormLabel (t (:label block))
                                              (when (-> block :props :required) "*"))

                                           ($ Select {:name (:name block)
                                                      :disabled (:disabled (:props block))
                                                      :onValueChange (aget % "field" "onChange")
                                                      :defaultValue (aget % "field" "value")}

                                              ($ FormControl
                                                 ($ SelectTrigger {:name (:name block)}
                                                    ($ SelectValue {:placeholder (:placeholder (:props block))})))

                                              ($ SelectContent {:data-test-id (str (:name block) "-options")}
                                                 (for [option (:options (:props block))]
                                                   ($ SelectItem {:key (:value option)
                                                                  :value (:value option)
                                                                  :class-name "cursor-pointer"}
                                                      ($ :button {:type "button"}
                                                         (:label option)))))
                                              ($ FormMessage)))})

                 ;; Calendar field 
                 "calendar"
                 ($ FormField {:control (cj control)
                               :name (:name block)
                               :render #($ FormItem {:class-name "flex flex-col mt-6"}
                                           ($ FormLabel (t (:label block))
                                              (when (-> block :props :required) "*"))
                                           (let [field-value (aget % "field" "value")]
                                             ($ Popover
                                                ($ PopoverTrigger {:asChild true}
                                                   ($ FormControl
                                                      ($ Button {:name (:name block)
                                                                 :disabled (:disabled (:props block))
                                                                 :variant "outline"
                                                                 :class-name "w-[240px] pl-3 text-left font-normal disabled:cursor-not-allowed"}
                                                         (if field-value
                                                           (format field-value "yyyy-MM-dd")
                                                           ($ :span {:class-name "text-muted-foreground"}
                                                              "Select date"))
                                                         ($ CalendarIcon {:class-name "ml-auto h-4 w-4 opacity-50"}))))

                                                ($ PopoverContent {:class-name "w-auto p-0"
                                                                   :align "start"}
                                                   ($ Calendar (merge {:captionLayout "dropdown"
                                                                       :onSelect (aget % "field" "onChange")
                                                                       :selected (aget % "field" "value")}
                                                                      (:props block))))))

                                           ($ FormMessage))})

                 ;; "default case - this renders a component from the component map"
                 (let [comp (get fields-map (:component block))]
                   (when comp
                     ($ FormField {:control (cj control)
                                   :name (:name block)
                                   :render #($ FormItem {:class-name "mt-6"}
                                               ($ FormLabel (t (:label block))
                                                  (when (-> block :props :required) "*"))
                                               ($ FormControl
                                                  ($ comp (merge
                                                           (:props block)
                                                           (:field (jc %)))))

                                               ($ FormDescription
                                                  ($ :<> (:description block)))

                                               ($ FormMessage))}))))))
         (when (:disabled (:props block))
           ($ TooltipContent {:side "left"
                              :class-name "w-[150px]"}
              (t "pool.packages.package.fields.protected")))))))
