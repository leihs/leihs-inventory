(ns leihs.inventory.client.routes.pools.inventory.templates.crud.components.fields
  (:require
   ["@@/dropzone" :refer [Dropzone]]
   ["@@/form" :refer [FormField FormItem FormLabel FormControl FormDescription FormMessage]]
   ["@@/input" :refer [Input]]
   ["@@/textarea" :refer [Textarea]]
   ["react-i18next" :refer [useTranslation]]
   [leihs.inventory.client.components.form.form-field-array :refer [FormFieldArray FormFieldArrayItems]]
   [leihs.inventory.client.components.form.select-model :refer [SelectModel]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.pools.inventory.templates.crud.components.model-item :refer [ModelItem]]
   [uix.core :as uix :refer [defui $]]))

(def fields-map
  {"input" Input
   "dropzone" Dropzone
   "textarea" Textarea})

(defui field [{:keys [control form block]}]
  (let [[t] (useTranslation)]
    (case (:component block)
      "models"
      ($ FormFieldArray {:form form
                         :name (:name block)}
         ($ FormItem {:class-name "mt-6"}
            ($ FormLabel (t (:label block)) (when (:required (:props block)) "*"))
            ($ SelectModel {:form form
                            :name (:name block)
                            :props (:props block)}))

         ($ FormDescription
            ($ :<> (:description block)))

         ($ FormMessage)

         ($ FormFieldArrayItems {:form form
                                 :name (:name block)}
            ($ ModelItem)))

      ;; "default case - this renders a component from the component map"
      (let [comp (get fields-map (:component block))]
        (when comp
          ($ FormField {:control (cj control)
                        :name (:name block)
                        :render #($ FormItem {:class-name "mt-6"}

                                    ($ FormLabel (t (:label block)) (when (:required (:props block)) "*"))
                                    ($ FormControl
                                       ($ comp (merge
                                                (dissoc (:props block) :required)
                                                (:field (jc %)))))

                                    ($ FormDescription
                                       ($ :<> (:description block)))

                                    ($ FormMessage))}))))))
