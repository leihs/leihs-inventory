(ns leihs.inventory.client.components.form.fields.checkbox-group-field
  (:require
   ["@@/checkbox-group" :refer [CheckboxGroup CheckboxGroupItem]]
   ["@@/field" :refer [FieldSet FieldLegend]]
   ["@@/form" :refer [FormControl FormField FormMessage]]
   ["react-i18next" :refer [useTranslation]]
   [uix.core :refer [$ defui]]))

(defui CheckboxGroupField [{:keys [form block class-name]}]
  (let [[t] (useTranslation)
        label (:label block)]
    ($ FormField {:control (.-control form)
                  :name (:name block)
                  :render #($ FieldSet {:class-name (str "mt-6 gap-0 " class-name)}
                              (when label
                                ($ FieldLegend {:variant "label"}
                                   label
                                   (when (-> block :props :required) "*")))

                              ($ FormControl
                                 ($ CheckboxGroup {:value (aget % "field" "value")
                                                   :onValueChange (aget % "field" "onChange")
                                                   :disabled (:disabled (:props block))}
                                    (for [option (:options (:props block))]
                                      ($ CheckboxGroupItem {:key (:value option)
                                                            :value (:value option)
                                                            :label (if (:bypass-i18n (:props block))
                                                                     (:label option)
                                                                     (t (:label option)))
                                                            :data-test-id (str (:name block) "-" (:value option))}))))
                              ($ FormMessage))})))
