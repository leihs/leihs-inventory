(ns leihs.inventory.client.components.form.fields.radio-group-field
  (:require
   ["@@/form" :refer [FormControl FormField FormItem FormLabel]]
   ["@@/radio-group" :refer [RadioGroup RadioGroupItem]]
   ["react-i18next" :refer [useTranslation]]
   [leihs.inventory.client.lib.utils :refer [cj]]
   [uix.core :refer [$ defui]]))

(defui RadioGroupField [{:keys [form block]}]
  (let [[t] (useTranslation)]
    ($ FormField {:control (.-control form)
                  :name (:name block)
                  :render #($ FormItem {:class-name "mt-6"}
                              ($ FormLabel (t (:label block))
                                 (when (-> block :props :required) "*"))

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
                                            (if (:bypass-i18n (:props block))
                                              (:label option)
                                              (t (:label option)))))))))})))
