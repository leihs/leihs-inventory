(ns leihs.inventory.client.routes.pools.models.crud.components.field-dispatcher
  (:require
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
  (cond
    (-> block :component (= "accessory-list"))
    ($ AccessoryListField {:form form
                           :block block})

    (-> block :component (= "entitlement-allocations"))
    ($ EntitlementAllocationsField {:block block
                                    :form form})

    (-> block :component (= "category-assignment"))
    ($ CategoryAssignmentField {:form form})

    (-> block :component (= "image-dropzone"))
    ($ ImageUploadField {:form form
                         :block block})

    (-> block :component (= "attachments"))
    ($ AttachmentsField {:form form
                         :name (:name block)
                         :props (:props block)})

    (-> block :component (= "compatible-models"))
    ($ CompatibleModelsField {:form form
                              :block block})

    (-> block :component (= "model-properties"))
    ($ ModelPropertiesField {:block block
                             :form form})

    (-> block :component (= "autocomplete"))
    ($ AutocompleteField {:form form
                          :name (:name block)
                          :label (:label block)
                          :props (merge
                                  {:remap (fn [item] {:value item
                                                      :label item})}
                                  (:props block))})

    (-> block :component (= "checkbox"))
    ($ CheckboxField {:form form
                      :block block})

      ;; "default case - this renders a component from the component map"
    :else
    ($ CommonField {:form form
                    :block block})))
