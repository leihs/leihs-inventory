(ns leihs.inventory.client.routes.pools.options.crud.components.field-dispatcher
  (:require
   ["react-i18next" :refer [useTranslation]]
   [leihs.inventory.client.components.form.fields.common-field :refer [CommonField]]
   [leihs.inventory.client.components.form.fields.price-field :refer [PriceField]]
   [uix.core :refer [$ defui]]))

(defui FieldDispatcher [{:keys [form block]}]
  (let [[t] (useTranslation)
        translated-block (if (:label block) (update block :label t) block)]
    (if (= (:component block) "price")
      ($ PriceField {:form form
                     :block translated-block})
      ($ CommonField {:form form
                      :block translated-block}))))
