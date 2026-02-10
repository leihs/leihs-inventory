(ns leihs.inventory.client.routes.pools.models.crud.components.model-item
  (:require
   ["@@/table" :refer [TableCell]]
   [leihs.inventory.client.components.form.form-field-array :refer [use-array-item]]
   [leihs.inventory.client.components.image-cell :refer [ImageCell]]
   [uix.core :as uix :refer [$ defui]]))

(defui ModelItem []
  (let [{:keys [field]} (use-array-item)]
    ($ :<>
       ;; Image cell with preview dialog
       ($ ImageCell {:field field})

       ;; Name cell
       ($ TableCell {:class-name ""}
          (str (:name field))))))
