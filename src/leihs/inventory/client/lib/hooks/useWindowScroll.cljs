(ns leihs.inventory.client.lib.hooks.useWindowScroll
  (:require [uix.core :as uix :refer [defhook]]))

(defhook use-window-scroll []
  (let [[state set-state!] (uix/use-state {:x nil :y nil})

        scroll-to (uix/use-callback
                   (fn [& args]
                     (cond
                       (object? (first args))
                       (.scrollTo js/window (first args))

                       (and (number? (first args)) (number? (second args)))
                       (.scrollTo js/window (first args) (second args))

                       :else
                       (throw (js/Error. "Invalid arguments passed to scrollTo. See here for more info. https://developer.mozilla.org/en-US/docs/Web/API/Window/scrollTo"))))
                   [])]

    (uix/use-layout-effect
     (fn []
       (let [handle-scroll (fn []
                             (set-state! {:x (.-scrollX js/window)
                                          :y (.-scrollY js/window)}))]
         (handle-scroll)
         (.addEventListener js/window "scroll" handle-scroll)

         (fn []
           (.removeEventListener js/window "scroll" handle-scroll))))
     [])

    [state scroll-to]))
