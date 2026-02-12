(ns leihs.inventory.client.routes.pools.inventory.templates.crud.components.field-dispatcher
  (:require
   [leihs.inventory.client.components.form.fields.common-field :refer [CommonField]]
   [leihs.inventory.client.routes.pools.inventory.templates.crud.components.fields.models-field :refer [ModelsField]]
   [uix.core :refer [$ defui]]))

(defui FieldDispatcher [{:keys [form block]}]
  (cond
    (-> block :component (= "models"))
    ($ ModelsField {:form form
                    :block block})

    ;; default case - renders a component from the component map
    :else
    ($ CommonField {:form form
                    :block block})))
