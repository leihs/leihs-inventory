(ns leihs.inventory.client.routes.pools.models.crud.components.field-dispatcher
  (:require
   ["react-i18next" :refer [useTranslation]]
   [leihs.inventory.client.components.form.fields.attachments-field :refer [AttachmentsField]]
   [leihs.inventory.client.components.form.fields.autocomplete-field :refer [AutocompleteField]]
   [leihs.inventory.client.components.form.fields.checkbox-field :refer [CheckboxField]]
   [leihs.inventory.client.components.form.fields.common-field :refer [CommonField]]
   [leihs.inventory.client.routes.pools.models.crud.components.fields.accessories-list-field :refer [AccessoryListField]]
   [leihs.inventory.client.routes.pools.models.crud.components.fields.category-assignment-field :refer [CategoryAssignmentField]]
   [leihs.inventory.client.routes.pools.models.crud.components.fields.compatible-models-field :refer [CompatibleModelsField]]
   [leihs.inventory.client.routes.pools.models.crud.components.fields.entitlement-allocations-field :refer [EntitlementAllocationsField]]
   [leihs.inventory.client.routes.pools.models.crud.components.fields.image-upload-field :refer [ImageUploadField]]
   [leihs.inventory.client.routes.pools.models.crud.components.fields.model-properties-field :refer [ModelPropertiesField]]
   [uix.core :as uix :refer [$ defui]]))

(defui FieldDispatcher [{:keys [form block]}]
  (let [[t] (useTranslation)
        translated-block (if (:label block) (update block :label t) block)]
    (cond
      (-> block :component (= "accessory-list"))
      ($ AccessoryListField {:form form
                             :block translated-block})

      (-> block :component (= "entitlement-allocations"))
      ($ EntitlementAllocationsField {:block translated-block
                                      :form form})

      (-> block :component (= "category-assignment"))
      ($ CategoryAssignmentField {:form form})

      (-> block :component (= "image-dropzone"))
      ($ ImageUploadField {:form form
                           :block translated-block})

      (-> block :component (= "attachments"))
      ($ AttachmentsField {:form form
                           :name (:name translated-block)
                           :props (:props translated-block)})

      (-> block :component (= "compatible-models"))
      ($ CompatibleModelsField {:form form
                                :block translated-block})

      (-> block :component (= "model-properties"))
      ($ ModelPropertiesField {:block translated-block
                               :form form})

      (-> block :component (= "autocomplete"))
      ($ AutocompleteField {:form form
                            :name (:name translated-block)
                            :label (:label translated-block)
                            :props (merge
                                    {:remap (fn [item] {:value item
                                                        :label item})}
                                    (:props translated-block))})

      (-> block :component (= "checkbox"))
      ($ CheckboxField {:form form
                        :block translated-block})

      ;; default case - renders a component from the component map
      :else
      ($ CommonField {:form form
                      :block translated-block}))))
