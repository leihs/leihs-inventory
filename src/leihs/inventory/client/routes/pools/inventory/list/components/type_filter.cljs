(ns leihs.inventory.client.routes.pools.inventory.list.components.type-filter
  (:require
   ["@@/badge" :refer [Badge]]
   ["@@/button" :refer [Button]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent
                               DropdownMenuRadioGroup DropdownMenuRadioItem
                               DropdownMenuTrigger]]
   ["lucide-react" :refer [Tags ChevronDown]]

   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [class-name]}]
  (let [[search-params set-search-params!] (router/useSearchParams) [t] (useTranslation)
        type (.. search-params (get "type"))
        handle-type (fn [selected-type]
                      (if (= selected-type type)
                        (.delete search-params "type")
                        (.set search-params "type" selected-type))

                      (.set search-params "page" "1")
                      (set-search-params! search-params))]

    ($ DropdownMenu
       ($ DropdownMenuTrigger {:asChild "true"}
          ($ Button {:variant "outline"}
             ($ Tags {:className (str "h-4 w-4 mr-2 " class-name)})
             (t "pool.models.filters.type.title")
             ($ ChevronDown {:className "ml-auto h-4 w-4 opacity-50"})))
       ($ DropdownMenuContent {:align "start"}
          ($ DropdownMenuRadioGroup {:value type
                                     :onValueChange handle-type}
             ($ DropdownMenuRadioItem {:value "model"}
                ($ :button {:type "button"}
                   ($ Badge {:class-name "bg-slate-500 hover:bg-slate-500"}
                      (t "pool.models.filters.type.model"))))

             ($ DropdownMenuRadioItem {:value "package"}
                ($ :button {:type "button"}
                   ($ Badge {:class-name "bg-lime-500 hover:bg-lime-500"}
                      (t "pool.models.filters.type.package"))))

             ($ DropdownMenuRadioItem {:value "option"}
                ($ :button {:type "button"}
                   ($ Badge {:class-name "bg-emerald-500 hover:bg-emerald-500"}
                      (t "pool.models.filters.type.option"))))

             ($ DropdownMenuRadioItem {:value "software"}
                ($ :button {:type "button"}
                   ($ Badge {:class-name "bg-orange-500 hover:bg-orange-500"}
                      (t "pool.models.filters.type.software")))))))))

(def TypeFilter
  (uix/as-react
   (fn [props]
     (main props))))
