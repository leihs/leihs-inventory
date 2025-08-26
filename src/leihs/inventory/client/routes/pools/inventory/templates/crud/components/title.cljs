(ns leihs.inventory.client.routes.pools.inventory.templates.crud.components.title
  (:require
   ["react-hook-form" :refer [useWatch]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui main [{:keys [control]}]
  (let [name (useWatch (clj->js {:control control
                                 :name "name"}))]
    ($ :<> (when (> (count name) 0) (str " – " name)))))
