(ns leihs.inventory.client.routes.models.components.reset
  (:require
   ["@@/button" :refer [Button]]
   ["lucide-react" :refer [ListRestart]]
   [leihs.inventory.client.routes.models.filter-reducer :as filter-reducer]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [class-name]}]
  (let [dispatch (filter-reducer/use-filter-dispatcher)]

    ($ Button {:size "icon"
               :variant "outline"
               :class-name (str "ml-2 " class-name)
               :on-click #(dispatch {:reset true})}
       ($ ListRestart))))

(def FilterReset
  (uix/as-react
   (fn [props]
     (main props))))
