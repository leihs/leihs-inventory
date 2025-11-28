(ns leihs.inventory.client.routes.pools.models.crud.components.fields
  (:require
   ["@@/checkbox" :refer [Checkbox]]
   ["@@/dropzone" :refer [Dropzone]]
   ["@@/form" :refer [FormField FormItem FormLabel FormControl FormDescription FormMessage]]
   ["@@/input" :refer [Input]]
   ["@@/textarea" :refer [Textarea]]
   ["react-i18next" :refer [useTranslation]]
   [leihs.inventory.client.components.form.attachments :refer [Attachments]]
   [leihs.inventory.client.components.form.instant-search :refer [InstantSearch]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.pools.models.crud.components.accessories-list :refer [AccessoryList]]
   [leihs.inventory.client.routes.pools.models.crud.components.category-assignment :refer [CategoryAssignment]]
   [leihs.inventory.client.routes.pools.models.crud.components.compatible-models :refer [CompatibleModels]]
   [leihs.inventory.client.routes.pools.models.crud.components.entitlement-allocations :refer [EntitlementAllocations]]
   [leihs.inventory.client.routes.pools.models.crud.components.image-upload :refer [ImageUpload]]
   [leihs.inventory.client.routes.pools.models.crud.components.model-properties :refer [ModelProperties]]
   [uix.core :as uix :refer [defui $]]))

(def fields-map
  {"input" Input
   "dropzone" Dropzone
   "textarea" Textarea})

(defui field [{:keys [control form block]}]
  (let [[t] (useTranslation)]
    (cond
      (-> block :component (= "accessory-list"))
      ($ AccessoryList {:control control
                        :props (:props block)})

      (-> block :component (= "entitlement-allocations"))
      ($ EntitlementAllocations {:control control
                                 :items "0"
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
      ($ CompatibleModels {:form form
                           :name (:name block)
                           :props (:props block)})

      (-> block :component (= "model-properties"))
      ($ ModelProperties {:control control
                          :props (:props block)})

      (-> block :component (= "instant-search"))
      ($ InstantSearch {:form form
                        :name (:name block)
                        :label (:label block)
                        :props (:props block)})

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
