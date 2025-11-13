(ns leihs.inventory.client.components.sticky-bottom
  (:require
   [leihs.inventory.client.lib.hooks.useWindowScroll :refer [use-window-scroll]]
   [uix.core :as uix :refer [$ defui]]))

(defn get-visible-height [container]
  ;; Get the viewport height
  (let [viewport-height (.-innerHeight js/window)
        ;; Get the bounding rectangle of the container
        rect (.getBoundingClientRect container)
        ;; Calculate the visible top and bottom
        visible-top (max 0 (.-top rect))
        visible-bottom (min viewport-height (.-bottom rect))]
    ;; Calculate and return the visible height
    (max 0 (- visible-bottom visible-top))))

(defui main [{:keys [children offset]}]
  (let [container-ref (uix/use-ref nil)
        sentinel-ref (uix/use-ref nil)
        [xy scroll-to] (use-window-scroll)
        [parent-height set-parent-height!] (uix/use-state 0)
        [bottom set-bottom!] (uix/use-state nil)
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
       (when-let [el (.-current container-ref)]
         (js/console.debug (get-visible-height (.. el -parentElement)))
         (set-bottom! (get-visible-height (.. el -parentElement)))
         (set-parent-height! (.. el -parentElement -offsetHeight))))
     [xy])

    ($ :div {:ref container-ref
             :class-name "sticky top-0 ml-auto"
             :style {:height bottom}}

       ($ :div {:class-name "relative"
                :style {:top "90%"}}
          children)

       ;; Sentinel element to detect bottom visibility
       ($ :div {:ref sentinel-ref
                :class-name "absolute bottom-0 h-[1px]"}))))

(def StickyBottom
  (uix/as-react
   (fn [props]
     (main props))))
