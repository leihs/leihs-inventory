(ns leihs.inventory.client.routes.pools.inventory.entitlement-groups.crud.components.group-item
  (:require
   ["@@/table" :refer [TableCell]]
   [leihs.inventory.client.components.form.form-field-array :refer [use-array-item]]
   [uix.core :as uix :refer [$ defui]]))

(defui GroupItem []
  (let [{:keys [field]} (use-array-item)]
    ($ TableCell {:class-name "pl-4"}
       (:name field))))
