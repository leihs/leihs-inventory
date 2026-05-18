(ns leihs.inventory.client.routes.pools.software.crud.components.field-dispatcher
  (:require
   ["react-i18next" :refer [useTranslation]]
   [leihs.inventory.client.components.form.fields.attachments-field :refer [AttachmentsField]]
   [leihs.inventory.client.components.form.fields.autocomplete-field :refer [AutocompleteField]]
   [leihs.inventory.client.components.form.fields.common-field :refer [CommonField]]
   [uix.core :refer [$ defui]]))

(defui FieldDispatcher [{:keys [form block]}]
  (let [[t] (useTranslation)
        translated-block (if (:label block) (update block :label t) block)]
    (cond
      (-> block :component (= "attachments"))
      ($ AttachmentsField {:form form
                           :name (:name translated-block)
                           :props (:props translated-block)})

      (-> block :component (= "autocomplete"))
      ($ AutocompleteField {:form form
                            :name (:name translated-block)
                            :label (:label translated-block)
                            :props (merge
                                    {:remap (fn [item] {:value item
                                                        :label item})}
                                    (:props translated-block))})

      ;; default case - renders a component from the component map
      :else
      ($ CommonField {:form form
                      :block translated-block}))))
