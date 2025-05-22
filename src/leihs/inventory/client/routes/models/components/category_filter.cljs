(ns leihs.inventory.client.routes.models.components.category-filter
  (:require
   ["@@/button" :refer [Button]]
   ["@@/collapsible" :refer [Collapsible CollapsibleContent CollapsibleTrigger]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent
                               DropdownMenuTrigger]]
   ["lucide-react" :refer [Check ChevronRight List]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router]
   [uix.core :as uix :refer [$ defui]]))

(defui file-tree-item [{:keys [name category_id children]}]
  (let [[search-params set-search-params!] (router/useSearchParams)
        handle-select (fn [e]
                        (let [id (.. e -currentTarget -value)]
                          (if (= id (.. search-params (get "category_id")))
                            (.delete search-params "category_id")
                            (.set search-params "category_id" id))
                          (set-search-params! search-params)))]

    (if (not (seq children))
      ($ :button {:role "button"
                  :value category_id
                  :on-click handle-select
                  :class-name "text-left text-sm flex items-center gap-2 pl-4 py-1"}
         ($ :div {:class-name "flex items-center space-x-2"}
            ($ :span
               ($ Check
                  {:class-name (str "h-4 w-4 "
                                    (if (= category_id (.. search-params (get "category_id")))
                                      "visible"
                                      "invisible"))}))
            ($ :span
               name)))

      ($ Collapsible {:class-name "pl-4"}
         ($ CollapsibleTrigger {:class-name "text-sm w-full group flex items-center gap-2 py-1"}
            ($ ChevronRight {:class-name "h-4 w-4 group-data-[state=open]:rotate-90 transition-transform"})
            ($ :span {:class-name "flex items-center gap-2"} name))
         ($ CollapsibleContent
            (for [child children]
              ($ file-tree-item (merge {:key (:category_id child)}
                                       child))))))))

(defui main [{:keys [class-name]}]
  (let [categories (:children (:categories (router/useRouteLoaderData "models-page")))
        [t] (useTranslation)]

    ($ DropdownMenu
       ($ DropdownMenuTrigger {:asChild "true"}
          ($ Button {:variant "outline"
                     :class-name class-name}
             ($ List {:className "h-4 w-4 mr-2 "})
             (t "pool.models.filters.categories.title")))
       ($ DropdownMenuContent {:class-name "h-[300px]"}
          ($ :div {:class-name "w-[300px] p-4 rounded-lg"}
             ($ :div {:class-name "w-full -ml-4"}
                (for [category categories]
                  ($ file-tree-item (merge {:key (:category_id category)}
                                           category)))))))))

(def CategoryFilter
  (uix/as-react
   (fn [props]
     (main props))))


