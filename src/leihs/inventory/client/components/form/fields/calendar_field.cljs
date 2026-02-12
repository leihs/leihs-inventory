(ns leihs.inventory.client.components.form.fields.calendar-field
  (:require
   ["@/components/ui/popover" :refer [Popover PopoverContent PopoverTrigger]]
   ["@@/button" :refer [Button]]
   ["@@/calendar" :refer [Calendar]]
   ["@@/form" :refer [FormControl FormField FormItem FormLabel FormMessage]]
   ["date-fns" :refer [format]]
   ["lucide-react" :refer [CalendarIcon]]
   ["react-i18next" :refer [useTranslation]]
   [leihs.inventory.client.lib.utils :refer [cj]]
   [uix.core :refer [$ defui]]))

(defui CalendarField [{:keys [form block]}]
  (let [[t] (useTranslation)]
    ($ FormField {:control (cj (.-control form))
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

                              ($ FormMessage))})))
