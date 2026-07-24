(ns leihs.inventory.client.components.form.fields.checkbox-field
  (:require
   ["@@/checkbox" :refer [Checkbox]]
   ["@@/form" :refer [FormControl FormField FormItem FormLabel FormMessage]]
   [leihs.inventory.client.lib.utils :refer [jc]]
   [uix.core :as uix :refer [$ defui]]))

(defui CheckboxField [{:keys [form block]}]
  ($ FormField {:control (.-control form)
                :name (:name block)
                :render (fn [field-props]
                          (let [field (-> (jc field-props) :field)
                                checkbox ($ FormControl
                                            ($ Checkbox (merge
                                                         {:checked (:value field)
                                                          :onCheckedChange (:onChange field)}
                                                         (:props block))))]
                            (if (:inline block)
                              ($ FormItem {:class-name "mt-6 flex flex-row items-center gap-2 space-y-0"}
                                 checkbox
                                 ($ :div
                                    ($ FormLabel
                                       (:label block))
                                    ($ FormMessage)))
                              ($ FormItem {:class-name "mt-6"}
                                 checkbox
                                 ($ FormLabel {:class-name "pl-4"}
                                    (:label block))
                                 ($ FormMessage)))))}))
