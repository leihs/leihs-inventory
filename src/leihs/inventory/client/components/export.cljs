(ns leihs.inventory.client.components.export
  (:require
   ["@@/button" :refer [Button]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuTrigger
                               DropdownMenuContent DropdownMenuItem]]
   ["@@/spinner" :refer [Spinner]]
   ["lucide-react" :refer [Download ChevronDown]]
   ["react-router-dom" :as router]
   [clojure.string :as str]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defn- effective-search [location-search custom-search]
  (if (nil? custom-search)
    location-search
    (let [s (str/trim (str custom-search))]
      (cond
        (= s "") location-search
        (str/starts-with? s "?") s
        :else (str "?" s)))))

(defui main [{:keys [url className class-name search variant disabled ml-auto? label-class-name label
                     data-test-id]
              :or {variant "outline"
                   ml-auto? true
                   label-class-name "hidden xl:flex items-center"
                   label "Export"}}]
  (let [location (router/useLocation)
        fetcher (router/useFetcher)
        location-search (.-search location)
        search-params (effective-search location-search search)
        full-url (str url search-params)
        trigger-disabled? (or disabled (= (.-state fetcher) "submitting"))
        extra-class (or class-name className "")
        button-class (str (when ml-auto? "ml-auto ") extra-class)]

    ($ DropdownMenu
       ($ DropdownMenuTrigger {:asChild true}
          ($ Button (cond-> {:variant variant
                             :disabled trigger-disabled?
                             :className button-class}
                      data-test-id (assoc :data-test-id data-test-id))
             (if (= (.-state fetcher) "idle")
               ($ Download {:className "h-4 w-4 xl:mr-2"})
               ($ Spinner {:className "h-4 w-4 xl:mr-2"}))
             ($ :div {:class-name label-class-name}
                label
                ($ ChevronDown {:className "h-4 w-4 ml-2"}))))

       ($ DropdownMenuContent
          ($ fetcher.Form {:method "post"
                           :action "/export"}
             ($ :input {:type "hidden"
                        :name "url"
                        :value full-url})
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
                        :value full-url})
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
