(ns leihs.inventory.client.routes.pools.items.review.components.serial-number
  (:require
   ["@@/button" :refer [Button]]
   ["@@/button-group" :refer [ButtonGroup]]
   ["@@/input-group" :refer [InputGroup InputGroupAddon InputGroupInput]]
   ["@@/spinner" :refer [Spinner]]
   ["@@/table" :refer [TableCell]]
   ["lucide-react" :refer [Save CircleCheck]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router]
   ["sonner" :refer [toast]]
   [clojure.string :as str]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [item poolId onSave]}]
  (let [fetcher (router/useFetcher)
        state (.-state fetcher)
        data (.-data fetcher)
        is-busy (or (= (.-state fetcher) "submitting")
                    (= (.-state fetcher) "loading"))

        serial-number (:serial_number item)
        [input set-input!] (uix/use-state (or serial-number ""))

        last-submission-data (uix/use-ref nil)
        button-ref (uix/use-ref nil)
        [t] (useTranslation)]

    ;; Watch fetcher state for completion and show toast
    (uix/use-effect
     (fn []
       (when (and (= state "idle")
                  (some? data)
                  (not= data @last-submission-data))

         ;; Store current data to prevent duplicate toasts
         (reset! last-submission-data data)

         (let [result data]
           (if (= (aget result "status") "ok")
             (do
               (.. toast (success (t "pool.items.review.serial_number.success")))
               ;; After successful save, focus next if flag is set
               (when onSave
                 (onSave)))

             (.. toast (error (t "pool.items.review.serial_number.error")))))))
     [state data onSave t])

    ($ TableCell
       ($ fetcher.Form {:method "patch"}
          ($ :input {:type "hidden"
                     :name "item-id"
                     :value (:id item)})
          ($ :input {:type "hidden"
                     :name "pool-id"
                     :value poolId})

          ($ ButtonGroup
             ($ InputGroup
                ($ InputGroupInput {:type "text"
                                    :name "serial_number"
                                    :data-sn-id (:id item)
                                    :placeholder (t "pool.items.review.serial_number.placeholder")
                                    :auto-complete "off"
                                    :value input
                                    :on-change #(set-input! (.. % -target -value))
                                    :disabled is-busy
                                    :class-name "w-48"})

                ($ InputGroupAddon {:align "inline-end"}
                   (if (and (not (str/blank? serial-number))
                            (= serial-number input))
                     ($ CircleCheck {:class-name "h-4 w-4 text-green-500"})
                     ($ CircleCheck {:class-name "invisible"}))))

             ($ Button {:type "submit"
                        :variant "outline"
                        :size "icon"
                        :ref button-ref
                        :disabled is-busy}

                (if is-busy
                  ($ Spinner {:class-name "h-4 w-4"})
                  ($ Save {:class-name "h-4 w-4"}))))))))

(def SerialNumber
  (uix/as-react
   (fn [props]
     (main props))))
