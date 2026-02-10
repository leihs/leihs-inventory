(ns leihs.inventory.client.components.export
  (:require
   ["@@/button" :refer [Button]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuTrigger
                               DropdownMenuContent DropdownMenuItem]]
   ["@@/spinner" :refer [Spinner]]
   ["lucide-react" :refer [Download ChevronDown]]
   ["react-router-dom" :as router]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui main [{:keys [url className]}]
  (let [location (router/useLocation)
        fetcher (router/useFetcher)
        search-params (.-search location)]

    ($ DropdownMenu
       ($ DropdownMenuTrigger {:asChild true}
          ($ Button {:variant "outline"
                     :disabled (= (.-state fetcher) "submitting")
                     :className (str "ml-auto " className)}

             (if (= (.-state fetcher) "idle")
               ($ Download {:className "h-4 w-4 xl:mr-2"})
               ($ Spinner {:className "h-4 w-4 xl:mr-2"}))
             ($ :div {:class-name "hidden xl:flex items-center"}
                "Export"
                ($ ChevronDown {:className "h-4 w-4 ml-2"}))))

       ($ DropdownMenuContent
          ($ fetcher.Form {:method "post"
                           :action "/export"}
             ($ :input {:type "hidden"
                        :name "url"
                        :value (str url search-params)})
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
                        :value (str url search-params)})
             ($ :input {:type "hidden"
                        :name "format"
                        :value "excel"})
             ($ DropdownMenuItem {:asChild true}
                ($ :button {:type "submit"
                            :className "w-full cursor-pointer"}
                   "Excel")))))))

(def Export
  (uix/as-react
   (fn [props]
     (main props))))
