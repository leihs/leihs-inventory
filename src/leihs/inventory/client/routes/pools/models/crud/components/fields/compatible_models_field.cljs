(ns leihs.inventory.client.routes.pools.models.crud.components.fields.compatible-models-field
  (:require
   ["@@/form" :refer [FormDescription FormField FormItem FormMessage]]
   ["@@/table" :refer [TableCell]]
   [leihs.inventory.client.components.form.form-field-array :refer [FormFieldArray
                                                                    FormFieldArrayItems
                                                                    use-array-item]]
   [leihs.inventory.client.components.form.select-model :refer [SelectModel]]
   [leihs.inventory.client.components.image-cell :refer [ImageCell]]
   [uix.core :as uix :refer [$ defui]]))

(defui ModelItem []
  (let [{:keys [field]} (use-array-item)]
    ($ :<>
       ;; Image cell with preview dialog
       ($ ImageCell {:field field})

       ;; Name cell
       ($ TableCell {:class-name ""}
          (str (:name field))))))

(defui CompatibleModelsField [{:keys [form block]}]
  ($ FormField {:control (aget form "control")
                :name (:name block)
                :render #($ FormFieldArray {:form form
                                            :name (:name block)}
                            ($ FormItem
                               ($ SelectModel {:form form
                                               :name (:name block)
                                               :props (:props block)})

                               ($ FormDescription
                                  ($ :<> (:description block)))

                               ($ FormMessage))

                            ($ FormFieldArrayItems {:form form
                                                    :name (:name block)}
                               ($ ModelItem)))}))
