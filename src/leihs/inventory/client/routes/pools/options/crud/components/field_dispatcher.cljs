(ns leihs.inventory.client.routes.pools.options.crud.components.field-dispatcher
  (:require
   [leihs.inventory.client.components.form.fields.common-field :refer [CommonField]]
   [uix.core :refer [$ defui]]))

(defui FieldDispatcher [{:keys [form block]}]
  ($ CommonField {:form form
                  :block block}))
