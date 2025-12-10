(ns leihs.inventory.client.lib.hooks
  (:require [uix.core :as uix :refer [defhook]]))

;; NOTE: would be nicer to user defhook macro, but somehow the linting is broken ( maybe beacause of uix version? ) 

;; NOTE: docs https://usehooks.com/usedebounce
(defn use-debounce [value delay]
  (let [[debounced-value set-debounced-value!] (uix/use-state value)]
    (uix/use-effect
     (fn []
       (let [handler (js/setTimeout
                      (fn [] (set-debounced-value! value))
                      delay)]

         (fn [] (js/clearTimeout handler))))
     [value delay])
    debounced-value))

;; NOTE: docs https://usehooks.com/useWindowScroll
(defn use-window-scroll []
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

;; NOTE: docs https://usehooks.com/useWindowSize
(defn use-window-size []
  (let [[size set-size!] (uix/use-state {:width nil :height nil})]

    (uix/use-layout-effect
     (fn []
       (let [handle-resize (fn []
                             (set-size! {:width (.-innerWidth js/window)
                                         :height (.-innerHeight js/window)}))]
         (handle-resize)
         (.addEventListener js/window "resize" handle-resize)

         (fn []
           (.removeEventListener js/window "resize" handle-resize))))
     [])

    size))

;; NOTE: docs https://www.30secondsofcode.org/react/s/use-mutation-observer/
(defn use-mutation-observer
  [{:keys [ref callback options]
    :or {options {:attributes false
                  :subtree false
                  :childList false
                  :characterData false}}}]

  (uix/use-effect
   (fn []
     (when-let [el (.-current ref)]
       (let [observer (js/MutationObserver. callback)]
         (.observe observer el (clj->js options))
         (fn []
           (.disconnect observer)))))
   [callback options ref]))

