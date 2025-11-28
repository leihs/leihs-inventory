(ns leihs.inventory.client.components.sticky-bottom
  (:require
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [children offset className]}]
  (let [container-ref (uix/use-ref nil)
        sentinel-ref (uix/use-ref nil)
        sticky-ref (uix/use-ref nil)
        [sticky-width set-sticky-width!] (uix/use-state nil)
        [parent-height set-parent-height!] (uix/use-state 0)
        [bottom-visible? set-bottom-visible!] (uix/use-state false)]

    ;; useEffect equivalent
    (uix/use-effect
     (fn []
       (let [observer (js/IntersectionObserver.
                       (fn [entries]
                         (let [entry (aget entries 0)]
                           (set-bottom-visible! (.-isIntersecting entry))))

                       #js {:root nil
                            :threshold 0})]
         (when-let [el (.-current sentinel-ref)]
           (.observe observer el))
          ;; cleanup function
         (fn [] (.disconnect observer))))
     [bottom-visible?])

    (uix/use-effect
     (fn []
       (when-let [el (.-current sticky-ref)]
         (set-sticky-width! (.. el -offsetWidth)))

       (when-let [el (.-current container-ref)]
         (set-parent-height! (.. el -parentElement -height))))
     [])

    ($ :div {:ref container-ref
             :class-name (str "relative " className)
             :style {:width sticky-width
                     :height parent-height}}

       ($ :div {:ref sticky-ref
                :style {:position (if bottom-visible? "absolute" "fixed")
                        :bottom (if bottom-visible? "0px" offset)
                        :transition "bottom 0.1s linear"}}
          children)

       ;; Sentinel element to detect bottom visibility
       ($ :div {:ref sentinel-ref
                :class-name "absolute bottom-0 h-[1px]"}))))

(def StickyBottom
  (uix/as-react
   (fn [props]
     (main props))))
