(ns leihs.inventory.client.routes.pools.inventory.entitlement-groups.crud.components.user-item
  (:require
   ["@@/table" :refer [TableCell]]
   [leihs.inventory.client.components.form.form-field-array :refer [use-array-item]]
   [uix.core :as uix :refer [$ defui]]))

(defui UserItem []
  (let [{:keys [field]} (use-array-item)
        user-label (str (:firstname field) " " (:lastname field)
                        (some->> (:email field) not-empty (str " - ")))]
    ($ TableCell {:class-name "pl-4"}
       user-label)))
