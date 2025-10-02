(ns leihs.inventory.client.routes.pools.inventory.list.components.filters.reset
  (:require
   ["@@/button" :refer [Button]]
   ["lucide-react" :refer [ListRestart]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui main [{:keys [className onReset]}]
  (let [ref (uix/use-ref nil)]

    (uix/use-effect
     (fn []
       (let [on-key-down
             (fn [e]
               (js/console.debug e)
               (when (and (= (.. e -code) "KeyR")
                          (.-altKey e)
                          (.-shiftKey e)
                          (not (.-ctrlKey e))
                          (not (.-metaKey e)))
                 (.preventDefault e)
                 (when ref
                   (when-let [input-element (.-current ref)]
                     (.. input-element (click))))))]

         (js/window.addEventListener "keydown" on-key-down)
         (fn [] (js/window.removeEventListener "keydown" on-key-down))))
     [])

    ($ Button {:ref ref
               :size "icon"
               :variant "outline"
               :className (str " " className)
               :on-click onReset}
       ($ ListRestart))))

(def Reset
  (uix/as-react
   (fn [props]
     (main props))))
