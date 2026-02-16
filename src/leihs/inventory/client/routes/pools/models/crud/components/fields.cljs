(ns leihs.inventory.client.routes.pools.models.crud.components.fields
  (:require
   ["@@/checkbox" :refer [Checkbox]]
   ["@@/dropzone" :refer [Dropzone]]
   ["@@/form" :refer [FormField FormItem FormLabel FormControl FormDescription FormMessage]]
   ["@@/input" :refer [Input]]
   ["@@/textarea" :refer [Textarea]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :refer [useLoaderData]]
   [leihs.inventory.client.components.form.attachments :refer [Attachments]]
   [leihs.inventory.client.components.form.autocomplete :refer [Autocomplete]]
   [leihs.inventory.client.components.form.form-field-array :refer [FormFieldArray FormFieldArrayItems]]
   [leihs.inventory.client.components.form.select-model :refer [SelectModel]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.pools.models.crud.components.accessories-list :refer [AccessoryList]]
   [leihs.inventory.client.routes.pools.models.crud.components.category-assignment :refer [CategoryAssignment]]
   [leihs.inventory.client.routes.pools.models.crud.components.entitlement-allocations :refer [EntitlementAllocations]]
   [leihs.inventory.client.routes.pools.models.crud.components.image-upload :refer [ImageUpload]]
   [leihs.inventory.client.routes.pools.models.crud.components.model-item :refer [ModelItem]]
   [leihs.inventory.client.routes.pools.models.crud.components.model-properties :refer [ModelProperties]]
   [uix.core :as uix :refer [defui $]]))

(def fields-map
  {"input" Input
   "dropzone" Dropzone
   "textarea" Textarea})

(defui field [{:keys [control form block]}]
  (let [[t] (useTranslation)
        {:keys [data]} (jc (useLoaderData))]
    (cond
      (-> block :component (= "accessory-list"))
      ($ AccessoryList {:control control
                        :props (:props block)})

      (-> block :component (= "entitlement-allocations"))
      ($ EntitlementAllocations {:control control
                                 :items (str (or (:rentable data) 0))
                                 :form form
                                 :props (:props block)})

      (-> block :component (= "category-assignment"))
      ($ CategoryAssignment {:control control
                             :form form
                             :props (:props block)})

      (-> block :component (= "image-dropzone"))
      ($ ImageUpload {:control control
                      :form form
                      :props (:props block)})

      (-> block :component (= "attachments"))
      ($ Attachments {:form form
                      :name (:name block)
                      :props (:props block)})

      (-> block :component (= "compatible-models"))
      ($ FormFieldArray {:form form
                         :name (:name block)}
         ($ FormItem {:class-name "mt-6"}
            ($ FormLabel (t (:label block)) (when (:required (:props block)) "*"))
            ($ SelectModel {:form form
                            :name (:name block)
                            :props (:props block)})

            ($ FormDescription
               ($ :<> (:description block)))

            ($ FormMessage))

         ($ FormFieldArrayItems {:form form
                                 :name (:name block)}
            ($ ModelItem)))

      (-> block :component (= "model-properties"))
      ($ ModelProperties {:control control
                          :props (:props block)})

      (-> block :component (= "autocomplete"))
      ($ Autocomplete {:form form
                       :name (:name block)
                       :label (:label block)
                       :props (merge
                               {:remap (fn [item] {:value item
                                                   :label item})}
                               (:props block))})

      (-> block :component (= "checkbox"))
      ($ FormField {:control (cj control)
                    :name (:name block)
                    :render #($ FormItem {:class-name "mt-6"}
                                ($ FormControl
                                   ($ Checkbox (merge
                                                {:checked (-> (jc %) :field :value)
                                                 :onCheckedChange (-> (jc %) :field :onChange)}
                                                (:props block))))

                                ($ FormLabel {:className "pl-4"} (t (:label block)))
                                ($ FormMessage))})

      ;; "default case - this renders a component from the component map"
      :else
      (let [comp (get fields-map (:component block))]
        (when comp
          ($ FormField {:control (cj control)
                        :name (:name block)
                        :render #($ FormItem {:class-name "mt-6"}

                                    ($ FormLabel (t (:label block)))
                                    ($ FormControl
                                       ($ comp (merge
                                                (:props block)
                                                (:field (jc %)))))

                                    ($ FormDescription
                                       ($ :<> (:description block)))

                                    ($ FormMessage))}))))))
