(ns leihs.inventory.client.routes.pools.inventory.list.components.filters.type-filter
  (:require
   ["@@/badge" :refer [Badge]]
   ["@@/button" :refer [Button]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent
                               DropdownMenuRadioGroup DropdownMenuRadioItem
                               DropdownMenuTrigger]]
   ["lucide-react" :refer [Tags ChevronDown]]

   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router]
   [clojure.string :as str]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [className]}]
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
          ($ Button {:class-name (str "min-w-48 w-48 " className)
                     :variant "outline"}

             ($ Tags {:className "h-4 w-4"})
             (t "pool.models.filters.type.title")
             (if type
               ($ Badge {:class-name (str "ml-auto w-6 justify-center "
                                          (case type
                                            "model"
                                            "bg-slate-500 hover:bg-slate-500"
                                            "package"
                                            "bg-lime-500 hover:bg-lime-500"
                                            "option"
                                            "bg-emerald-500 hover:bg-emerald-500"
                                            "software"
                                            "bg-orange-500 hover:bg-orange-500 "))}
                  (str/upper-case (subs type 0 1)))
               ($ ChevronDown {:className "ml-auto h-4 w-4 opacity-50"}))))

       ($ DropdownMenuContent {:class-name "min-w-48 pr-2"
                               :data-test-id "type-filter-dropdown"
                               :align "start "}
          ($ DropdownMenuRadioGroup {:value type
                                     :onValueChange handle-type}
             ($ DropdownMenuRadioItem {:value "model"}
                ($ :button {:type "button"}
                   ($ Badge {:class-name "bg-slate-500 hover:bg-slate-500 mr-2 w-6 justify-center "}
                      "M") (t "pool.models.filters.type.model")))

             ($ DropdownMenuRadioItem {:value "package"}
                ($ :button {:type "button"}
                   ($ Badge {:class-name "bg-lime-500 hover:bg-lime-500 mr-2 w-6 justify-center "}
                      "P") (t "pool.models.filters.type.package")))

             ($ DropdownMenuRadioItem {:value "option"}
                ($ :button {:type "button"}
                   ($ Badge {:class-name "bg-emerald-500 hover:bg-emerald-500 mr-2 w-6 justify-center"}
                      "O") (t "pool.models.filters.type.option")))

             ($ DropdownMenuRadioItem {:value "software"}
                ($ :button {:type "button"}
                   ($ Badge {:class-name "bg-orange-500 hover:bg-orange-500 mr-2 w-6 justify-center"}
                      "S") (t "pool.models.filters.type.software"))))))))

(def TypeFilter
  (uix/as-react
   (fn [props]
     (main props))))
