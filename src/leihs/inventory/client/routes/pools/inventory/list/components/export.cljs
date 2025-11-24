(ns leihs.inventory.client.routes.pools.inventory.list.components.export
  (:require
   ["@@/button" :refer [Button]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuTrigger
                               DropdownMenuContent DropdownMenuItem]]
   ["lucide-react" :refer [Download ChevronDown]]
   ["react-router-dom" :as router :refer [Form]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui main [{:keys [className]}]
  (let [location (router/useLocation)
        fetcher (router/useFetcher)
        current-path (.-pathname location)
        search-params (.-search location)]

    (js/console.debug current-path)

    ($ DropdownMenu
       ($ DropdownMenuTrigger {:asChild true}
          ($ Button {:variant "outline"
                     :className (str "ml-auto " className)}
             ($ Download {:className "h-4 w-4 mr-2"})
             "Export"
             ($ ChevronDown {:className "h-4 w-4 ml-2"})))

       ($ DropdownMenuContent
          ($ fetcher.Form {:method "post"
                           :action current-path}
             ($ :input {:type "hidden"
                        :name "url"
                        :value (str current-path search-params)})
             ($ :input {:type "hidden"
                        :name "format"
                        :value "csv"})
             ($ DropdownMenuItem {:asChild true}
                ($ :button {:type "submit"
                            :className "w-full cursor-pointer"}
                   "CSV")))

          ($ fetcher.Form {:method "post"
                           :action current-path}
             ($ :input {:type "hidden"
                        :name "url"
                        :value (str current-path search-params)})
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
