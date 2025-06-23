(ns leihs.inventory.client.routes.items.crud.components.fields
  (:require
   ["@/components/ui/popover" :refer [Popover PopoverContent PopoverTrigger]]
   ["@@/button" :refer [Button]]
   ["@@/calendar" :refer [Calendar]]
   ["@@/checkbox" :refer [Checkbox]]
   ["@@/dropzone" :refer [Dropzone]]
   ["@@/form" :refer [FormControl FormDescription FormField FormItem FormLabel
                      FormMessage]]
   ["@@/input" :refer [Input]]
   ["@@/radio-group" :refer [RadioGroup RadioGroupItem]]
   ["@@/select" :refer [Select SelectContent SelectItem SelectTrigger
                        SelectValue]]
   ["@@/textarea" :refer [Textarea]]
   ["date-fns" :refer [format]]
   ["lucide-react" :refer [CalendarIcon]]
   [leihs.inventory.client.components.form.attachments :refer [Attachments]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.items.crud.components.inventory-code :refer [InventoryCode]]
   [leihs.inventory.client.routes.items.crud.components.models :refer [Models]]
   [uix.core :as uix :refer [$ defui]]))

(def fields-map
  {"input" Input
   "dropzone" Dropzone
   "textarea" Textarea})

(defui field [{:keys [control form block]}]
  (cond
    (-> block :component (= "inventory-code"))
    ($ InventoryCode {:control control
                      :props (:props block)})

    (-> block :component (= "attachments"))
    ($ Attachments {:form form
                    :props (:props block)})

    (-> block :component (= "models"))
    ($ Models {:control control
               :form form
               :block block})

    (-> block :component (= "checkbox"))
    ($ FormField {:control (cj control)
                  :name (:name block)
                  :render #($ FormItem {:class-name "mt-6"}
                              ($ FormControl
                                 ($ Checkbox (merge
                                              {:name (:name block)
                                               :checked (-> (jc %) :field :value)
                                               :onCheckedChange (-> (jc %) :field :onChange)}
                                              (:props block))))

                              ($ FormLabel {:className "pl-4"} (:label block))
                              ($ FormMessage))})

    ;; Radiogroup field
    (-> block :component (= "radio-group"))
    ($ FormField {:control (cj control)
                  :name (:name block)
                  :render #($ FormItem {:class-name "mt-6"}
                              ($ FormLabel (:label block))

                              ($ FormControl
                                 ($ RadioGroup {:onValueChange (aget % "field" "onChange")
                                                :defaultValue (aget % "field" "value")
                                                :class-name "flex space-x-1"
                                                :name (:name block)}

                                    (for [option (:options (:props block))]
                                      ($ FormItem {:key (:value option)
                                                   :class-name "flex items-center space-x-2 space-y-0"}
                                         ($ FormControl
                                            ($ RadioGroupItem {:value (:value option)}))
                                         ($ FormLabel {:class-name "font-normal"}
                                            (:label option)))))))})

    ;; Select field
    (-> block :component (= "select"))
    ($ FormField {:control (cj control)
                  :name (:name block)
                  :render #($ FormItem {:class-name "mt-6"}
                              ($ FormLabel (:label block))

                              ($ Select {:onValueChange (aget % "field" "onChange")
                                         :defaultValue (aget % "field" "value")}

                                 ($ FormControl
                                    ($ SelectTrigger {:name (:name block)}
                                       ($ SelectValue {:placeholder (:placeholder (:props block))})))

                                 ($ SelectContent
                                    (for [option (:options (:props block))]
                                      ($ SelectItem {:key (:value option)
                                                     :value (:value option)}
                                         ($ :<> (:label option)))))
                                 ($ FormMessage)))})

    ;; Calendar field 
    (-> block :component (= "calendar"))
    ($ FormField {:control (cj control)
                  :name (:name block)
                  :render #($ FormItem {:class-name "flex flex-col mt-6"}
                              ($ FormLabel (:label block))
                              (let [field-value (aget % "field" "value")]
                                ($ Popover
                                   ($ PopoverTrigger {:asChild true}
                                      ($ FormControl
                                         ($ Button {:name (:name block)
                                                    :variant "outline"
                                                    :class-name "w-[240px] pl-3 text-left font-normal"}
                                            (if field-value
                                              (format field-value "PPP")
                                              ($ :span {:class-name "text-muted-foreground"}
                                                 "Select date"))
                                            ($ CalendarIcon {:class-name "ml-auto h-4 w-4 opacity-50"}))))

                                   ($ PopoverContent {:class-name "w-auto p-0"
                                                      :align "start"}
                                      ($ Calendar (merge {:onSelect (aget % "field" "onChange")
                                                          :selected (aget % "field" "value")}
                                                         (:props block))))))

                              ($ FormMessage))})

    ;; "default case - this renders a component from the component map"
    :else
    (let [comp (get fields-map (:component block))]
      (when comp
        ($ FormField {:control (cj control)
                      :name (:name block)
                      :render #($ FormItem {:class-name "mt-6"}
                                  ($ FormLabel (:label block))
                                  ($ FormControl
                                     ($ comp (merge
                                              (:props block)
                                              (:field (jc %)))))

                                  ($ FormDescription
                                     ($ :<> (:description block)))

                                  ($ FormMessage))})))))
