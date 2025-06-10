(ns leihs.inventory.client.routes.models.components.type-filter
  (:require
   ["@@/badge" :refer [Badge]]
   ["@@/button" :refer [Button]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent
                               DropdownMenuRadioGroup DropdownMenuRadioItem
                               DropdownMenuTrigger]]
   ["lucide-react" :refer [Tags ChevronDown]]

   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router]
   [leihs.inventory.client.routes.models.filter-reducer :as filter-reducer]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [class-name]}]
  (let [[search-params set-search-params!] (router/useSearchParams) [t] (useTranslation)

        dispatch (filter-reducer/use-filter-dispatcher)
        state (filter-reducer/use-filter-state)

        type (.. search-params (get "type"))
        handle-type (fn [selected-type]
                      (if (= selected-type type)
                        (dispatch {:filter "type"
                                   :value nil
                                   :active false})

                        (dispatch {:filter "type"
                                   :value selected-type
                                   :active true})))]

    ($ DropdownMenu
       ($ DropdownMenuTrigger {:asChild "true"}
          ($ Button {:name "type-filter"
                     :class-name (when (-> state :hidden :type) "hidden ")

                     :variant "outline"}
             ($ Tags {:className (str "h-4 w-4 mr-2 " class-name)})
             (t "pool.models.filters.type.title")
             ($ ChevronDown {:className "ml-auto h-4 w-4 opacity-50"})))
       ($ DropdownMenuContent {:align "start"}
          ($ DropdownMenuRadioGroup {:value type
                                     :onValueChange handle-type}
             ($ DropdownMenuRadioItem {:value "model"}
                ($ :button {:type "button"
                            :name "model-filter"}
                   ($ Badge {:class-name "bg-slate-500 hover:bg-slate-500"}
                      (t "pool.models.filters.type.model"))))

             ($ DropdownMenuRadioItem {:value "package"}
                ($ :button {:type "button"
                            :name "package-filter"}
                   ($ Badge {:class-name "bg-lime-500 hover:bg-lime-500"}
                      (t "pool.models.filters.type.package"))))

             ($ DropdownMenuRadioItem {:value "option"}
                ($ :button {:type "button"
                            :name "option-filter"}
                   ($ Badge {:class-name "bg-emerald-500 hover:bg-emerald-500"}
                      (t "pool.models.filters.type.option"))))

             ($ DropdownMenuRadioItem {:value "software"}
                ($ :button {:type "button"
                            :name "software-filter"}
                   ($ Badge {:class-name "bg-orange-500 hover:bg-orange-500"}
                      (t "pool.models.filters.type.software")))))))))

(def TypeFilter
  (uix/as-react
   (fn [props]
     (main props))))
