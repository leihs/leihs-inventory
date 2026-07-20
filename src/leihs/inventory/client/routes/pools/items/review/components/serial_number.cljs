(ns leihs.inventory.client.routes.pools.items.review.components.serial-number
  (:require
   ["@@/button" :refer [Button]]
   ["@@/button-group" :refer [ButtonGroup]]
   ["@@/input-group" :refer [InputGroup InputGroupAddon InputGroupInput]]
   ["@@/spinner" :refer [Spinner]]
   ["@@/table" :refer [TableCell]]
   ["lucide-react" :refer [Save CircleCheck]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router" :as router]

   ["sonner" :refer [toast]]
   [clojure.string :as str]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [$ defui]]))

(defui SerialNumber [{:keys [item pool-id on-save]}]
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
           (case (aget result "httpStatus")
             200
             (do
               (.. toast (success (t "pool.items.review.serial_number.success")))
               ;; After successful save, focus next if flag is set
               (when on-save
                 (on-save)))

             409
             (let [errors (jc (.-errors data))
                   serial-err (first (filter #(= (:code %) "DUPLICATE_SERIAL_NUMBER") errors))]
               (if serial-err
                 (.. toast (error (t "pool.items.review.serial_number.duplicate_error")
                                  (cj {:duration 20000
                                       :description (t "pool.items.review.serial_number.description")
                                       :action {:label (t "pool.items.review.serial_number.overwrite")
                                                :onClick (fn []
                                                           (.submit fetcher
                                                                    #js {"item-id" (:id item)
                                                                         "pool-id" pool-id
                                                                         "serial_number" input
                                                                         "on_conflict_serial_number" "overwrite"}
                                                                    #js {:method "patch"}))}})))

                 (.. toast (error (t "pool.items.review.serial_number.error")
                                  (cj {:description (t "error.action.error_detail"
                                                       #js {:httpStatus (aget result "httpStatus")})})))))

             (.. toast (error (t "pool.items.review.serial_number.error")
                              (cj {:description (t "error.action.error_detail"
                                                   #js {:httpStatus (aget result "httpStatus")})})))))))

     [state data on-save t fetcher item pool-id input])

    ($ TableCell
       ($ fetcher.Form {:method "patch"}
          ($ :input {:type "hidden"
                     :name "item-id"
                     :value (:id item)})
          ($ :input {:type "hidden"
                     :name "pool-id"
                     :value pool-id})

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

