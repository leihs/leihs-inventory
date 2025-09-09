(ns leihs.inventory.client.routes.pools.inventory.list.page
  (:require
   ["@@/button" :refer [Button]]
   ["@@/card" :refer [Card CardContent CardFooter CardHeader]]
   ["@@/table" :refer [Table TableBody TableCell TableHead TableHeader TableRow]]
   ["lucide-react" :refer [Download ListRestart]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router]
   [clojure.string :as str]
   [leihs.inventory.client.components.pagination :as pagination]
   [leihs.inventory.client.routes.pools.inventory.list.components.export :refer [Export]]
   [leihs.inventory.client.routes.pools.inventory.list.components.filters.before-last-check-filter :refer [BeforeLastCheckFilter]]
   [leihs.inventory.client.routes.pools.inventory.list.components.filters.borrowable-filter :refer [BorrowableFilter]]
   [leihs.inventory.client.routes.pools.inventory.list.components.filters.category-filter :refer [CategoryFilter]]
   [leihs.inventory.client.routes.pools.inventory.list.components.filters.inventory-pool-filter :refer [InventoryPoolFilter]]
   [leihs.inventory.client.routes.pools.inventory.list.components.filters.reset :refer [Reset]]
   [leihs.inventory.client.routes.pools.inventory.list.components.filters.retired-filter :refer [RetiredFilter]]
   [leihs.inventory.client.routes.pools.inventory.list.components.filters.search-filter :as search :refer [SearchFilter]]
   [leihs.inventory.client.routes.pools.inventory.list.components.filters.status-filter :refer [StatusFilter]]
   [leihs.inventory.client.routes.pools.inventory.list.components.filters.type-filter :refer [TypeFilter]]
   [leihs.inventory.client.routes.pools.inventory.list.components.filters.with-items-filter :refer [WithItemsFilter]]
   [leihs.inventory.client.routes.pools.inventory.list.components.table.model-row :refer [ModelRow]]
   [leihs.inventory.client.routes.pools.inventory.list.components.table.skeleton-row :refer [SkeletonRow]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui page []
  (let [{:keys [data]} (router/useLoaderData)
        models (:data data)
        pagination (:pagination data)
        last-page-rows (let [mod-result (mod (:total_rows pagination) (:size pagination))]
                         (if (zero? mod-result)
                           (:size pagination)
                           mod-result))
        [to-last-page? set-to-last-page!] (uix/use-state false)
        [t] (useTranslation)
        navigate (router/useNavigate)
        navigation (router/useNavigation)
        loading-list? (and (= (.-state navigation) "loading")
                           (str/includes? (.. navigation -location -pathname) "/list"))
        handle-reset (fn []
                       (navigate "?page=1&size=50&with_items=true"))]

    (uix/use-effect
     (fn []
       (let [search (or (some-> navigation .-location .-search) nil)
             params (if search (js/URLSearchParams. search) nil)]
         (if (and params (= (.. params (get "page"))
                            (str (:total_pages pagination))))
           (set-to-last-page! true)
           (set-to-last-page! false))))
     [navigation pagination])

    ($ Card {:className "my-4"}
       ($ CardHeader {:className "flex bg-white rounded-xl z-10"
                      :style {:background "linear-gradient(to bottom, white 90%, transparent 100%)"}}
          ($ :div {:class-name "w-full flex"}
             ($ :div {:class-name "flex flex-col gap-2"}
                ($ :div {:className "flex gap-2"}
                   ($ SearchFilter)

                   ($ RetiredFilter)
                   ($ WithItemsFilter)
                   ($ BorrowableFilter))

                ($ :div {:className "flex gap-2"}
                   ($ TypeFilter)
                   ($ StatusFilter)
                   ($ InventoryPoolFilter)
                   ($ CategoryFilter)
                   ($ BeforeLastCheckFilter)
                   ($ Reset {:on-reset handle-reset})))

             ($ Export)))

       ($ CardContent {:class-name "pb-0"}
          ($ :div {:class-name "border rounded-md"}
             (if (empty? models)
               ($ :div {:class-name "p-4 text-center text-sm text-muted-foreground"}
                  (t "pool.models.list.empty"))
               ($ Table {:class-name "border-separate border-spacing-0 rounded-md"}
                  ($ TableHeader {:class-name "bg-white sticky top-16 rounded-t-md z-50"
                                  :style {:box-shadow "0 0.5px 0 hsl(var(--border))"}}
                     ($ TableRow {:class-name "rounded-t-md hover:bg-white"}
                        ($ TableHead {:class-name "rounded-tl-md text-right"}
                           (t "pool.models.list.header.quantity"))
                        ($ TableHead "")
                        ($ TableHead "")
                        ($ TableHead {:className "w-full"} (t "pool.models.list.header.name"))
                        ($ TableHead {:className "min-w-40 text-right"} (t "pool.models.list.header.availability"))
                        ($ TableHead {:class-name "rounded-tr-md"} "")))

                  ($ TableBody
                     (if loading-list?
                       (doall (for [i (range (if to-last-page?
                                               last-page-rows
                                               (:size pagination)))]
                                ($ SkeletonRow {:key i})))
                       (for [model models]
                         ($ ModelRow {:key (:id model)
                                      :model model}))))))))

       ($ CardFooter {:class-name "sticky bottom-0 bg-white z-10 rounded-b-xl  pt-6"
                      :style {:background "linear-gradient(to top, white 80%, transparent 100%)"}}
          ($ pagination/main {:pagination pagination
                              :class-name "justify-start w-full"})))))
