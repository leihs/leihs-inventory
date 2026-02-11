(ns leihs.inventory.client.routes.pools.packages.crud.components.item-item
  (:require
   ["@@/table" :refer [TableCell]]
   [leihs.inventory.client.components.form.form-field-array :refer [use-array-item]]
   [leihs.inventory.client.components.image-cell :refer [ImageCell]]
   [uix.core :as uix :refer [$ defui]]))

(defui ItemItem []
  (let [{:keys [field]} (use-array-item)]
    ($ :<>
       ;; Image cell with preview dialog
       ($ ImageCell {:field field})

       ;; Inventory Code cell
       ($ TableCell
          ($ :div {:class-name "flex flex-col"}
             ($ :strong (:inventory_code field))
             (:model_name field))))))
