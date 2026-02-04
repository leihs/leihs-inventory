(ns leihs.inventory.client.routes.pools.inventory.list.page
  (:require
   ["@@/button" :refer [Button]]
   ["@@/card" :refer [Card CardContent CardFooter CardHeader]]
   ["@@/popover" :refer [Popover PopoverContent PopoverTrigger]]
   ["@@/table" :refer [Table TableBody TableHead TableHeader TableRow]]
   ["lucide-react" :refer [FunnelPlus]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router]
   [clojure.string :as str]
   [leihs.inventory.client.components.export :refer [Export]]
   [leihs.inventory.client.components.pagination :as pagination]
   [leihs.inventory.client.lib.hooks :as hooks]
   [leihs.inventory.client.routes.pools.inventory.list.components.filters.before-last-check-filter :refer [BeforeLastCheckFilter]]
   [leihs.inventory.client.routes.pools.inventory.list.components.filters.borrowable-filter :refer [BorrowableFilter]]
   [leihs.inventory.client.routes.pools.inventory.list.components.filters.category-filter :refer [CategoryFilter]]
   [leihs.inventory.client.routes.pools.inventory.list.components.filters.inventory-pool-filter :refer [InventoryPoolFilter]]
   [leihs.inventory.client.routes.pools.inventory.list.components.filters.reset :refer [Reset]]
   [leihs.inventory.client.routes.pools.inventory.list.components.filters.retired-filter :refer [RetiredFilter]]
   [leihs.inventory.client.routes.pools.inventory.list.components.filters.search-filter :refer [SearchFilter]]
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
        params (router/useParams)
        pool-id (aget params "pool-id")

        [to-last-page? set-to-last-page!] (uix/use-state false)
        [t] (useTranslation)
        navigate (router/useNavigate)
        navigation (router/useNavigation)
        loading-list? (and (= (.-state navigation) "loading")
                           (str/includes? (.. navigation -location -pathname) "/list"))
        [query-params _] (router/useSearchParams)
        size (js/parseInt (or (.. query-params (get "size"))
                              "50"))
        handle-reset (fn []
                       (navigate (str "?page=1&size=" size "&with_items=true")))
        is-desktop? (hooks/use-media-query "(min-width: 1280px)")]

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
       ($ CardHeader {:className "flex bg-background rounded-xl z-10 sticky top-16"
                      :style {:background "linear-gradient(to bottom, hsl(var(--background)) 90%, transparent 100%)"}}

          ;; Mobile filters
          (when-not is-desktop?
            ($ :div {:class-name "w-full flex gap-2"}
               ($ SearchFilter)

               ($ Popover
                  ($ PopoverTrigger {:as-child true}
                     ($ Button {:variant "outline"
                                :size "icon"}
                        ($ FunnelPlus)))
                  ($ PopoverContent {:class-name "w-max"}
                     ($ :div {:class-name "flex flex-col gap-2"}
                        ($ RetiredFilter)
                        ($ WithItemsFilter {:class-name "w-full"})
                        ($ BorrowableFilter {:class-name "w-full"})
                        ($ TypeFilter {:class-name "w-full"})
                        ($ StatusFilter {:class-name "!max-w-full"})
                        ($ InventoryPoolFilter {:class-name "!max-w-full"})

                        ($ CategoryFilter {:class-name "w-full"})
                        ($ BeforeLastCheckFilter {:class-name "!max-w-full"})
                        ($ Reset {:class-name "mt-2 w-full"
                                  :on-reset handle-reset}))))
               ($ Export {:url (str "/inventory/" pool-id "/list/")})))

          ;; Desktop filters
          (when is-desktop?
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

               ($ Export {:url (str "/inventory/" pool-id "/list/")}))))

       ($ CardContent {:class-name "pb-0"}
          ($ :div {:class-name "border rounded-md"}
             (if (empty? models)
               ($ :div {:class-name "p-4 text-center text-sm text-muted-foreground"}
                  (t "pool.models.list.empty"))
               ($ :div {:class-name "overflow-x-auto"}
                  ($ Table
                     ($ TableHeader {:class-name "rounded-t-md"}
                        ($ TableRow {:class-name "rounded-t-md hover:bg-background border-b"}
                           ($ TableHead {:class-name "rounded-tl-md text-right"}
                              (t "pool.models.list.header.quantity"))
                           ($ TableHead "")
                           ($ TableHead "")
                           ($ TableHead {:className "w-full min-w-[600px]"}
                              (t "pool.models.list.header.name"))
                           ($ TableHead {:className "min-w-40 text-right "}
                              (t "pool.models.list.header.availability"))
                           ($ TableHead {:class-name "rounded-tr-md sticky"} "")))

                     ($ TableBody
                        (if loading-list?
                          (doall (for [i (range (if to-last-page?
                                                  last-page-rows
                                                  (:size pagination)))]
                                   ($ SkeletonRow {:key i})))
                          (for [model models]
                            ($ ModelRow {:key (:id model)
                                         :model model})))))))))

       ($ CardFooter {:class-name "sticky bottom-0 bg-background z-10 rounded-b-xl  pt-6"
                      :style {:background "linear-gradient(to top, hsl(var(--background)) 80%, transparent 100%)"}}
          ($ pagination/main {:pagination pagination
                              :class-name "justify-start w-full"})))))
