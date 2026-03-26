(ns leihs.inventory.client.components.form.fields.checkbox-group-field
  (:require
   ["@@/checkbox-group" :refer [CheckboxGroup CheckboxGroupItem]]
   ["@@/field" :refer [FieldSet FieldLegend]]
   ["@@/form" :refer [FormControl FormField FormMessage]]
   ["react-i18next" :refer [useTranslation]]
   [uix.core :refer [$ defui]]))

(defui CheckboxGroupField [{:keys [form block]}]
  (let [[t] (useTranslation)]
    ($ FormField {:control (.-control form)
                  :name (:name block)
                  :render #($ FieldSet {:class-name "mt-6"}
                              ($ FieldLegend {:variant "label"}
                                 (t (:label block))
                                 (when (-> block :props :required) " *"))

                              (js/console.debug (:props block))
                              ($ FormControl
                                 ($ CheckboxGroup {:value (aget % "field" "value")
                                                   :onValueChange (aget % "field" "onChange")
                                                   :disabled (:disabled (:props block))}
                                    (for [option (:options (:props block))]
                                      ($ CheckboxGroupItem {:key (:value option)
                                                            :value (:value option)
                                                            :label (:label option)
                                                            :data-test-id (str (:name block) "-" (:value option))}))))
                              ($ FormMessage))})))
