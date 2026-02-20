(ns leihs.inventory.client.routes.pools.inventory.templates.crud.components.model-item
  (:require
   ["@@/input" :refer [Input]]
   ["@@/table" :refer [TableCell]]
   ["react-i18next" :refer [useTranslation]]
   [leihs.inventory.client.components.form.form-field-array :refer [use-array-item]]
   [leihs.inventory.client.components.image-cell :refer [ImageCell]]
   [leihs.inventory.client.lib.utils :refer [cj]]
   [uix.core :as uix :refer [$ defui]]))

(defui ModelItem []
  (let [[t] (useTranslation)
        {:keys [field update index form]} (use-array-item)]
    ($ :<>
       ;; Image cell with preview dialog
       ($ ImageCell {:field field})

       ;; Name cell
       ($ TableCell {:class-name ""}
          (str (:name field)))

       ($ TableCell {:class-name "w-1/5"}

          (cond
            (and (:quantity field)
                 (> (:quantity field)
                    (:borrowable_quantity field)))
            ($ :span {:class-name "text-red-500"}
               (t "pool.templates.template.quantity_error"))

            (and (:quantity field)
                 (< (:quantity field) 0))
            (let [models-err (aget (aget form "formState" "errors") "models")]
              (when (and models-err (aget models-err index))
                ($ :span {:class-name "text-red-500"}
                   (aget models-err index "quantity" "message"))))))

       ($ TableCell {:class-name "w-[5rem]"}
          ($ Input {:type "number"
                    :data-test-id "quantity"
                    :value (if (:quantity field)
                             (:quantity field)
                             1)
                    :onChange (fn [event]
                                (update
                                 index
                                 (cj (merge field
                                            {:quantity (.. event -target -value)}))))}))

       ($ TableCell {:class-name "px-0"} "/")

       ($ TableCell (:borrowable_quantity field)))))
