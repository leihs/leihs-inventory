(ns leihs.inventory.client.components.form.fields.select-field
  (:require
   ["@@/form" :refer [FormControl FormField FormItem FormLabel FormMessage]]
   ["@@/select" :refer [Select SelectContent SelectItem SelectTrigger
                        SelectValue]]
   ["react-i18next" :refer [useTranslation]]
   [uix.core :refer [$ defui]]))

(defui SelectField [{:keys [form block]}]
  (let [[t] (useTranslation)]
    ($ FormField {:control (.-control form)
                  :name (:name block)
                  :render #($ FormItem {:class-name "mt-6"}
                              ($ FormLabel (t (:label block))
                                 (when (-> block :props :required) "*"))

                              ($ Select {:name (:name block)
                                         :disabled (:disabled (:props block))
                                         :onValueChange (aget % "field" "onChange")
                                         :value (aget % "field" "value")}

                                 ($ FormControl
                                    ($ SelectTrigger {:name (:name block)}
                                       ($ SelectValue {:placeholder (:placeholder (:props block))})))

                                 ($ SelectContent {:data-test-id (str (:name block) "-options")}
                                    (for [option (:options (:props block))]
                                      ($ SelectItem {:key (:value option)
                                                     :value (:value option)
                                                     :class-name "cursor-pointer"}
                                         ($ :button {:type "button"}
                                            (t (:label option))))))
                                 ($ FormMessage)))})))
