(ns leihs.inventory.client.routes.models.components.status-filter
  (:require
   ["@@/button" :refer [Button]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent
                               DropdownMenuRadioGroup DropdownMenuRadioItem
                               DropdownMenuTrigger]]
   ["lucide-react" :refer [CirclePlus]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [class-name]}]
  (let [[search-params set-search-params!] (router/useSearchParams)
        [t] (useTranslation)
        with_items (.. search-params (get "with_items"))
        status (let [owned (.. search-params (get "owned"))
                     in_stock (.. search-params (get "in_stock"))
                     incomplete (.. search-params (get "incomplete"))
                     broken (.. search-params (get "broken"))]
                 (cond
                   owned "owned"
                   in_stock "in_stock"
                   incomplete "incomplete"
                   broken "broken"))

        handle-status (fn [e]
                        (let [status (.. search-params (get e))]

                          (.delete search-params "in_stock")
                          (.delete search-params "owned")
                          (.delete search-params "incomplete")
                          (.delete search-params "broken")

                          (if status
                            (.delete search-params e)
                            (.set search-params e true))
                          (set-search-params! search-params)))]

    ;; remove item status when with_items false is selected
    (uix/use-effect
     (fn []
       (when (= with_items "false")
         (.delete search-params "in_stock")
         (.delete search-params "owned")
         (.delete search-params "incomplete")
         (.delete search-params "broken")
         (set-search-params! search-params)))
     [search-params set-search-params! with_items])

    ($ DropdownMenu
       ($ DropdownMenuTrigger {:asChild "true"}
          ($ Button {:variant "outline"
                     :disabled (= with_items "false")
                     :class-name class-name}
             ($ CirclePlus {:className "h-4 w-4 mr-2 "})
             (t "pool.models.filters.status.title")))
       ($ DropdownMenuContent {:align "start"}
          ($ DropdownMenuRadioGroup {:value status
                                     :onValueChange handle-status}
             ($ DropdownMenuRadioItem {:value "owned"}
                (t "pool.models.filters.status.owned"))

             ($ DropdownMenuRadioItem {:value "in_stock"}
                (t "pool.models.filters.status.in_stock"))

             ($ DropdownMenuRadioItem {:value "incomplete"}
                (t "pool.models.filters.status.incomplete"))

             ($ DropdownMenuRadioItem {:value "broken"}
                (t "pool.models.filters.status.broken")))))))

(def StatusFilter
  (uix/as-react
   (fn [props]
     (main props))))
