(ns leihs.inventory.client.routes.pools.models.crud.components.fields.model-properties-field
  (:require
   ["@@/button" :refer [Button]]
   ["@@/form" :refer [FormControl FormDescription FormField FormItem
                      FormMessage]]
   ["@@/input" :refer [Input]]
   ["@@/table" :refer [TableCell TableHead TableHeader TableRow]]
   ["@@/textarea" :refer [Textarea]]
   ["lucide-react" :refer [CirclePlus]]
   ["react-i18next" :refer [useTranslation]]
   [leihs.inventory.client.components.form.form-field-array :refer [FormFieldArray
                                                                    FormFieldArrayItems
                                                                    use-array-item
                                                                    use-array-items]]
   [leihs.inventory.client.lib.utils :refer [jc]]
   [uix.core :as uix :refer [$ defui]]))

;; Table header component
(defui PropertyTableHeader []
  (let [[t] (useTranslation)]
    ($ TableHeader
       ($ TableRow
          ($ TableHead (t "pool.model.model_properties.blocks.model_properties.table.property"))
          ($ TableHead (t "pool.model.model_properties.blocks.model_properties.table.value"))
          ($ TableHead "")))))

;; Add button component
(defui AddProperty []
  (let [[t] (useTranslation)
        {:keys [fields append]} (use-array-items)
        handle-add (fn []
                     (append #js {:id nil
                                  :key ""
                                  :value ""})
                     (let [name (str "textarea[name='properties." (count fields) ".key']")
                           next (js/document.querySelector name)]
                       (when next (.focus next))))]
    ($ Button {:type "button"
               :variant "outline"
               :on-click handle-add}
       ($ CirclePlus {:className "w-4 h-4"})
       (t "pool.model.model_properties.blocks.model_properties.add_button"))))

(defui PropertyItem []
  (let [{:keys [form name index]} (use-array-item)
        control (.-control form)]
    ($ :<>
       ;; Hidden ID field
       ($ TableCell {:class-name "hidden"}
          ($ FormField
             {:control control
              :name (str name "." index ".id")
              :render #($ FormItem
                          ($ FormControl
                             ($ Input (merge
                                       {:className "min-h-[2.5rem]"}
                                       (:field (jc %)))))
                          ($ FormMessage))}))

       ;; Key field (property name)
       ($ TableCell {:class-name "align-top"}
          ($ FormField
             {:control control
              :name (str name "." index ".key")
              :render #($ FormItem
                          ($ FormControl
                             ($ Textarea (merge
                                          {:className "min-h-[2.5rem]"
                                           :autoscale true
                                           :resize true}
                                          (:field (jc %)))))
                          ($ FormMessage))}))

       ;; Value field
       ($ TableCell {:class-name "align-top"}
          ($ FormField
             {:control control
              :name (str name "." index ".value")
              :render #($ FormItem
                          ($ FormControl
                             ($ Textarea (merge
                                          {:className "min-h-[2.5rem]"
                                           :autoscale true
                                           :resize true}
                                          (:field (jc %)))))
                          ($ FormMessage))})))))

(defui ModelPropertiesField [{:keys [block form]}]
  ($ FormField {:control (.-control form)
                :name (:name block)
                :render #($ FormFieldArray {:form form
                                            :name (:name block)}

                            ($ FormFieldArrayItems {:form form
                                                    :name (:name block)
                                                    :header ($ PropertyTableHeader)}
                               ($ PropertyItem))

                            ($ FormItem
                               ($ FormControl
                                  ($ AddProperty))

                               ($ FormDescription
                                  ($ :<> (:description block)))

                               ($ FormMessage)))}))
