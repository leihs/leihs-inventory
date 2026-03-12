(ns leihs.inventory.client.routes.pools.inventory.templates.crud.components.fields.models-field
  (:require
   ["@@/form" :refer [FormItem FormLabel FormDescription FormMessage
                      FormControl FormField]]
   ["@@/input" :refer [Input]]
   ["@@/table" :refer [TableCell]]
   ["react-i18next" :refer [useTranslation]]
   [leihs.inventory.client.components.form.form-field-array :refer [FormFieldArray FormFieldArrayItems
                                                                    use-array-item]]
   [leihs.inventory.client.components.form.select-model :refer [SelectModel]]
   [leihs.inventory.client.components.image-cell :refer [ImageCell]]
   [leihs.inventory.client.lib.utils :refer [jc]]
   [uix.core :refer [$ defui]]))

(defui ModelItem []
  (let [[t] (useTranslation)
        {:keys [field index form]} (use-array-item)]
    ($ :<>
       ;; Image cell with preview dialog
       ($ ImageCell {:field field})

       ;; Name cell
       ($ TableCell {:class-name ""}
          (str (:name field)))

       ($ TableCell {:class-name "w-1/5"}

          (cond
            (and (:quantity field)
                 (> (:quantity field)
                    (:borrowable_quantity field)))
            ($ :span {:class-name "text-red-500"}
               (t "pool.templates.template.quantity_error"))

            (and (:quantity field)
                 (< (:quantity field) 0))
            (let [models-err (aget (aget form "formState" "errors") "models")]
              (when (and models-err (aget models-err index))
                ($ :span {:class-name "text-red-500"}
                   (aget models-err index "quantity" "message"))))))

       ($ TableCell {:class-name "w-[5rem]"}
          ($ FormField
             {:control (.-control form)
              :name (str "models." index ".quantity")
              :render #($ FormItem
                          ($ FormControl
                             ($ Input (merge
                                       {:class-name "text-right"
                                        :type "number"
                                        :data-test-id "quantity"}
                                       (:field (jc %))))))}))

       ($ TableCell {:class-name "px-0"} "/")

       ($ TableCell (:borrowable_quantity field)))))

(defui ModelsField [{:keys [form block]}]
  (let [[t] (useTranslation)]
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
          ($ ModelItem)))))
