(ns leihs.inventory.client.components.form.fields.common-field
  (:require
   ["@@/dropzone" :refer [Dropzone]]
   ["@@/form" :refer [FormControl FormDescription FormField FormItem FormLabel
                      FormMessage]]
   ["@@/input" :refer [Input]]
   ["@@/textarea" :refer [Textarea]]
   ["react-i18next" :refer [useTranslation]]
   [leihs.inventory.client.lib.utils :refer [jc]]
   [uix.core :as uix :refer [$ defui]]))

(def fields-map
  {"input" Input
   "dropzone" Dropzone
   "textarea" Textarea})

(defui CommonField [{:keys [form block]}]
  (let [comp (get fields-map (:component block))
        [t] (useTranslation)]
    (when comp
      ($ FormField {:control (.-control form)
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

                                ($ FormMessage))}))))
