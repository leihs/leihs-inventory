(ns leihs.inventory.client.routes.pools.software.crud.components.field-dispatcher
  (:require
   [leihs.inventory.client.components.form.fields.attachments-field :refer [AttachmentsField]]
   [leihs.inventory.client.components.form.fields.autocomplete-field :refer [AutocompleteField]]
   [leihs.inventory.client.components.form.fields.common-field :refer [CommonField]]
   [uix.core :refer [$ defui]]))

(defui FieldDispatcher [{:keys [form block]}]
  (cond
    (-> block :component (= "attachments"))
    ($ AttachmentsField {:form form
                         :name (:name block)
                         :props (:props block)})

    (-> block :component (= "autocomplete"))
    ($ AutocompleteField {:form form
                          :name (:name block)
                          :label (:label block)
                          :props (merge
                                  {:remap (fn [item] {:value item
                                                      :label item})}
                                  (:props block))})

      ;; default case - renders a component from the component map
    :else
    ($ CommonField {:form form
                    :block block})))
