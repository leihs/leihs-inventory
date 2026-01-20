(ns leihs.inventory.client.routes.pools.items.review.components.serial-number
  (:require
   ["@@/button" :refer [Button]]
   ["@@/button-group" :refer [ButtonGroup]]
   ["@@/input" :refer [Input]]
   ["@@/table" :refer [TableCell]]
   ["lucide-react" :refer [Save]]
   ["react-router-dom" :as router]
   ["sonner" :refer [toast]]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [item poolId]}]
  (let [fetcher (router/useFetcher)
        state (.-state fetcher)
        data (.-data fetcher)
        is-submitting (= (.-state fetcher) "submitting")]

    ;; Watch fetcher state for completion and show toast
    (uix/use-effect
     (fn []
       (when (and (= (.-state fetcher) "idle")
                  (some? (.-data fetcher)))
         (let [result (.-data fetcher)]
           (if (= (aget result "status") "ok")
             (.. toast (success "Serial number updated"))
             (.. toast (error "Failed to update serial number")))))
       js/undefined)
     [state data fetcher])

    ($ TableCell
       ($ fetcher.Form {:method "patch"}
          ($ ButtonGroup
             ($ :<>
                ($ Input {:type "text"
                          :name "serial_number"
                          :placeholder "Enter serial number"
                          :defaultValue (:serial_number item)
                          :auto-complete "off"
                          :disabled is-submitting
                          :class-name "w-48"})
                ($ :input {:type "hidden"
                           :name "item-id"
                           :value (:id item)})
                ($ :input {:type "hidden"
                           :name "pool-id"
                           :value poolId}))
             ($ Button {:type "submit"
                        :variant "outline"
                        :size "icon"
                        :disabled is-submitting}
                ($ Save)))))))

(def SerialNumber
  (uix/as-react
   (fn [props]
     (main props))))
