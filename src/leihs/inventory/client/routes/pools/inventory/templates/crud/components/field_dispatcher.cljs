(ns leihs.inventory.client.routes.pools.inventory.templates.crud.components.field-dispatcher
  (:require
   ["react-i18next" :refer [useTranslation]]
   [leihs.inventory.client.components.form.fields.common-field :refer [CommonField]]
   [leihs.inventory.client.routes.pools.inventory.templates.crud.components.fields.models-field :refer [ModelsField]]
   [uix.core :refer [$ defui]]))

(defui FieldDispatcher [{:keys [form block]}]
  (let [[t] (useTranslation)
        translated-block (if (:label block) (update block :label t) block)]
    (cond
      (-> block :component (= "models"))
      ($ ModelsField {:form form
                      :block translated-block})

      ;; default case - renders a component from the component map
      :else
      ($ CommonField {:form form
                      :block translated-block}))))
