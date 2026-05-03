(ns leihs.inventory.client.components.form.fields.select-field
  (:require
   ["@@/form" :refer [FormControl FormField FormItem FormLabel FormMessage]]
   ["@@/select" :refer [Select SelectContent SelectItem SelectTrigger
                        SelectValue]]
   ["react-i18next" :refer [useTranslation]]
   [uix.core :refer [$ defui]]))

(defui SelectField [{:keys [form block class-name]}]
  (let [[t] (useTranslation)
        label (:label block)]
    ($ FormField {:control (.-control form)
                  :name (:name block)
                  :render #($ FormItem {:class-name (str "mt-6 " class-name)}
                              (when label
                                ($ FormLabel (t label)
                                   (when (-> block :props :required) "*")))

                              ($ Select {:name (:name block)
                                         :disabled (:disabled (:props block))
                                         :onValueChange (aget % "field" "onChange")
                                         :value (aget % "field" "value")
                                         :class-name "text-ellipsis overflow-x-hidden"}

                                 ($ FormControl
                                    ($ SelectTrigger {:name (:name block)}
                                       ($ SelectValue {:placeholder (:placeholder (:props block))})))

                                 ($ SelectContent {:data-test-id (str (:name block) "-options")}
                                    (for [option (:options (:props block))]
                                      ($ SelectItem {:key (:value option)
                                                     :value (:value option)
                                                     :class-name "cursor-poiner"}
                                         ($ :button {:type "button"}
                                            (if (:bypass-i18n (:props block))
                                              (:label option)
                                              (t (:label option)))))))
                                 ($ FormMessage)))})))
