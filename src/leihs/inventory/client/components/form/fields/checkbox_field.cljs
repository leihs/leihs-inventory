(ns leihs.inventory.client.components.form.fields.checkbox-field
  (:require
   ["@@/checkbox" :refer [Checkbox]]
   ["@@/form" :refer [FormControl FormField FormItem FormLabel FormMessage]]
   ["react-i18next" :refer [useTranslation]]
   [leihs.inventory.client.lib.utils :refer [jc]]
   [uix.core :as uix :refer [$ defui]]))

(defui CheckboxField [{:keys [form block]}]
  (let [[t] (useTranslation)]
    ($ FormField {:control (.-control form)
                  :name (:name block)
                  :render #($ FormItem {:class-name "mt-6"}
                              ($ FormControl
                                 ($ Checkbox (merge
                                              {:checked (-> (jc %) :field :value)
                                               :onCheckedChange (-> (jc %) :field :onChange)}
                                              (:props block))))

                              ($ FormLabel {:className "pl-4"}
                                 (t (:label block)))
                              ($ FormMessage))})))
