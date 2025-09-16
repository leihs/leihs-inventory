(ns leihs.inventory.client.routes.pools.inventory.list.page
  (:require
   ["@@/badge" :refer [Badge]]
   ["@@/button" :refer [Button]]
   ["@@/card" :refer [Card CardContent CardHeader CardFooter]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent
                               DropdownMenuItem DropdownMenuTrigger]]
   ["@@/table" :refer [Table TableBody TableCell TableHead TableHeader
                       TableRow]]
   ["lucide-react" :refer [Download Ellipsis Image ListRestart]]
   ["react" :as react :refer [Suspense]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [Link Await]]
   [leihs.inventory.client.components.pagination :as pagination]
   [leihs.inventory.client.routes.pools.inventory.list.components.before-last-check-filter :refer [BeforeLastCheckFilter]]
   [leihs.inventory.client.routes.pools.inventory.list.components.borrowable-filter :refer [BorrowableFilter]]
   [leihs.inventory.client.routes.pools.inventory.list.components.category-filter :refer [CategoryFilter]]
   [leihs.inventory.client.routes.pools.inventory.list.components.filter-indicator :refer [FilterIndicator]]
   [leihs.inventory.client.routes.pools.inventory.list.components.inventory-pool-filter :refer [InventoryPoolFilter]]
   [leihs.inventory.client.routes.pools.inventory.list.components.retired-filter :refer [RetiredFilter]]
   [leihs.inventory.client.routes.pools.inventory.list.components.search-filter :as search :refer [SearchFilter]]
   [leihs.inventory.client.routes.pools.inventory.list.components.status-filter :refer [StatusFilter]]
   [leihs.inventory.client.routes.pools.inventory.list.components.table.model-row :refer [ModelRow]]
   [leihs.inventory.client.routes.pools.inventory.list.components.table.skeleton-row :refer [SkeletonRow]]
   [leihs.inventory.client.routes.pools.inventory.list.components.type-filter :refer [TypeFilter]]
   [leihs.inventory.client.routes.pools.inventory.list.components.with-items-filter :refer [WithItemsFilter]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui page []
  (let [data (router/useLoaderData)
        models (:data data)
        pagination (:pagination data)
        [t] (useTranslation)
        ;; location (router/useLocation)
        navigate (router/useNavigate)
        ;; fetcher (router/useFetcher)
        handle-reset (fn []
                       (navigate "?page=1&size=50&with_items=true"))]

    ;; (uix/use-effect
    ;;  (fn []
    ;;    (js/console.debug "hello" (.. fetcher -state) (.. fetcher -data))
    ;;    (when (and (= (.. fetcher -state) "idle")
    ;;               (not (.. fetcher -data)))
    ;;      (js/console.debug "hello fetcher")
    ;;      (.. fetcher (load (str (.. location -pathname) (.. location -search))))))
    ;;  [location fetcher])

    (uix/use-effect
     (fn []
       (js/console.debug "data changed" (.. data -data)))
     [data])

    ($ Card {:className "my-4"}
       ($ CardHeader {:className "flex bg-white rounded-xl z-10"
                      :style {:background "linear-gradient(to bottom, white 90%, transparent 100%)"}}
          ($ :div
             ($ :div {:className "flex gap-2"}
                ($ SearchFilter)
                ($ TypeFilter)
                ($ StatusFilter)
                ($ InventoryPoolFilter)
                ($ CategoryFilter)
                ($ BeforeLastCheckFilter)

                ($ Button {:variant "outline" :className "ml-auto"}
                   ($ Download {:className "h-4 w-4 mr-2"}) "Export"))

             ($ :div {:className "flex gap-2 mt-2"}
                ($ RetiredFilter)
                ($ WithItemsFilter)
                ($ BorrowableFilter)
                ($ Button {:size "icon"
                           :variant "outline"
                           :class-name "ml-2"
                           :on-click handle-reset}
                   ($ ListRestart)))

             ($ :div {:className "flex space-x-2 mt-2"}
                ($ FilterIndicator))))

       ($ CardContent {:class-name "pb-0"}
          ($ :div {:class-name "border rounded-md"}
             ($ Table {:class-name "border-separate border-spacing-0 rounded-md"}
                ($ TableHeader {:class-name "bg-white sticky top-16 rounded-t-md z-50"
                                :style {:box-shadow "0 0.5px 0 hsl(var(--border))"}}
                   ($ TableRow {:class-name "rounded-t-md hover:bg-white"}
                      ($ TableHead {:class-name "rounded-tl-md text-right"}
                         (t "pool.models.list.header.amount"))
                      ($ TableHead "")
                      ($ TableHead "")
                      ($ TableHead {:className "w-full"} (t "pool.models.list.header.name"))
                      ($ TableHead {:className "min-w-40 text-right"} (t "pool.models.list.header.availability"))
                      ($ TableHead {:class-name "rounded-tr-md"} "")))

                ($ TableBody
                   ($ Suspense {:fallback ($ :tr ($ :td "hello"))}
                      ($ Await {:resolve (.. data -data)}
                         (fn [models]
                           (js/console.debug "models" models)
                           #_(for [model (:data models)]
                               ($ ModelRow {:key (:id model)
                                            :model model})))))))))

       ($ CardFooter {:class-name "sticky bottom-0 bg-white z-10 rounded-b-xl  pt-6"
                      :style {:background "linear-gradient(to top, white 80%, transparent 100%)"}}
          ($ pagination/main {:pagination pagination
                              :class-name "justify-start w-full"})))))
