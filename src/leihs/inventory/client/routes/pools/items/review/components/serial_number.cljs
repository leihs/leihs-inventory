(ns leihs.inventory.client.routes.pools.items.review.components.serial-number
  (:require
   ["@@/button" :refer [Button]]
   ["@@/button-group" :refer [ButtonGroup]]
   ["@@/input" :refer [Input]]
   ["@@/table" :refer [TableCell]]
   ["lucide-react" :refer [Save]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router]
   ["sonner" :refer [toast]]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [item poolId]}]
  (let [fetcher (router/useFetcher)
        state (.-state fetcher)
        data (.-data fetcher)
        is-submitting (= (.-state fetcher) "submitting")

        [t] (useTranslation)]

    ;; Watch fetcher state for completion and show toast
    (uix/use-effect
     (fn []
       (when (and (= (.-state fetcher) "idle")
                  (some? (.-data fetcher)))
         (let [result (.-data fetcher)]
           (if (= (aget result "status") "ok")
             (.. toast (success (t "pool.items.review.serial_number.success")))
             (.. toast (error (t "pool.items.review.serial_number.error"))))))
       js/undefined)
     [state data fetcher t])

    ($ TableCell
       ($ fetcher.Form {:method "patch"}
          ($ ButtonGroup
             ($ :<>
                ($ Input {:type "text"
                          :name "serial_number"
                          :placeholder (t "pool.items.review.serial_number.placeholder")
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
