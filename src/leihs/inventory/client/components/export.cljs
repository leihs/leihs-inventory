(ns leihs.inventory.client.components.export
  (:require
   ["@@/badge" :refer [Badge]]
   ["@@/button" :refer [Button]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuTrigger
                               DropdownMenuContent DropdownMenuItem]]
   ["@@/spinner" :refer [Spinner]]
   ["lucide-react" :refer [Download ChevronDown]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router]
   ["sonner" :refer [toast]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui Export [{:keys [url count className]}]
  (let [fetcher (router/useFetcher)
        last-data (uix/use-ref nil)
        [t] (useTranslation)]

    (uix/use-effect
     (fn []
       (let [data (.-data fetcher)
             state (.-state fetcher)]
         (when (and (= state "idle")
                    (some? data)
                    (not= data @last-data))
           (reset! last-data data)
           (when (= (aget data "status") "error")
             (.. toast (error (t "error.action.error")
                              (clj->js {:description (t "error.action.error_detail"
                                                        #js {:httpStatus (aget data "httpStatus")})})))))))
     [fetcher t])

    ($ DropdownMenu
       ($ DropdownMenuTrigger {:asChild true}
          ($ Button {:data-test-id "export-button"
                     :variant "outline"
                     :disabled (= (.-state fetcher) "submitting")
                     :className (str "ml-auto " className)}

             (if (= (.-state fetcher) "idle")
               ($ Download {:className "h-4 w-4 xl:mr-2"})
               ($ Spinner {:className "h-4 w-4 xl:mr-2"}))
             ($ :div {:class-name "hidden xl:flex items-center"}
                "Export"
                (when count
                  ($ Badge {:variant "primary"
                            :class-name "ml-2 rounded-full"}
                     count))
                ($ ChevronDown {:className "h-4 w-4 ml-2"}))))

       ($ DropdownMenuContent
          ($ fetcher.Form {:method "post"
                           :action "/export"}
             ($ :input {:type "hidden"
                        :name "url"
                        :value url})
             ($ :input {:type "hidden"
                        :name "format"
                        :value "csv"})
             ($ DropdownMenuItem {:asChild true}
                ($ :button {:type "submit"
                            :className "w-full cursor-pointer"}
                   "CSV")))

          ($ fetcher.Form {:method "post"
                           :action "/export"}
             ($ :input {:type "hidden"
                        :name "url"
                        :value url})
             ($ :input {:type "hidden"
                        :name "format"
                        :value "excel"})
             ($ DropdownMenuItem {:asChild true}
                ($ :button {:type "submit"
                            :className "w-full cursor-pointer"}
                   "Excel")))))))
