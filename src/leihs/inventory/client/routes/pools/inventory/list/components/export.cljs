(ns leihs.inventory.client.routes.pools.inventory.list.components.export
  (:require
   ["@@/button" :refer [Button]]
   ["lucide-react" :refer [Download]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui main [{:keys [className]}]
  ($ Button {:variant "outline"
             :className (str "ml-auto " className)}
     ($ Download {:className "h-4 w-4 mr-2"}) "Export"))

(def Export
  (uix/as-react
   (fn [props]
     (main props))))
