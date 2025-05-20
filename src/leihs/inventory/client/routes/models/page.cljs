(ns leihs.inventory.client.routes.models.page
  (:require
   ["@@/badge" :refer [Badge]]
   ["@@/button" :refer [Button]]
   ["@@/card" :refer [Card CardContent CardHeader]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent
                               DropdownMenuItem DropdownMenuTrigger]]
   ["@@/table" :refer [Table TableBody TableCell TableHead TableHeader
                       TableRow]]
   ["lucide-react" :refer [Download Ellipsis Image Tags ListRestart]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [Link]]
   [goog.functions]
   [leihs.inventory.client.components.pagination :as pagination]
   [leihs.inventory.client.routes.models.components.before-last-check-filter :refer [BeforeLastCheckFilter]]
   [leihs.inventory.client.routes.models.components.borrowable-filter :refer [BorrowableFilter]]
   [leihs.inventory.client.routes.models.components.category-filter :refer [CategoryFilter]]
   [leihs.inventory.client.routes.models.components.inventory-pool-filter :refer [InventoryPoolFilter]]
   [leihs.inventory.client.routes.models.components.retired-filter :refer [RetiredFilter]]
   [leihs.inventory.client.routes.models.components.search-filter :refer [SearchFilter]]
   [leihs.inventory.client.routes.models.components.status-filter :refer [StatusFilter]]
   [leihs.inventory.client.routes.models.components.type-filter :refer [TypeFilter]]
   [leihs.inventory.client.routes.models.components.with-items-filter :refer [WithItemsFilter]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defn find-category-name [tree-list category-id]
  (when tree-list
    (some (fn [node]
            (if (= (:category_id node) category-id)
              (:name node)
              (find-category-name (:children node) category-id)))
          tree-list)))

(defui page [{:keys [data]}]
  (let [models (:data (:models (router/useLoaderData)))
        [t] (useTranslation)
        location (router/useLocation)
        categories (:children (:categories (router/useRouteLoaderData "models-page")))
        inventory-pools (:available_inventory_pools (router/useRouteLoaderData "root"))
        [search-params set-search-params!] (router/useSearchParams)
        pagination (:pagination (:models (router/useLoaderData)))
        handle-reset (fn []
                       (.delete search-params "owned")
                       (.delete search-params "in_stock")
                       (.delete search-params "incomplete")
                       (.delete search-params "broken")
                       (.delete search-params "before_last_check")
                       (.delete search-params "category_id")
                       (.delete search-params "inventory_pool_id")
                       (.delete search-params "type")
                       (.delete search-params "retired")
                       (.delete search-params "borrowable")
                       (.delete search-params "search")

                       (.set search-params "with_items" "true")
                       (.set search-params "page" 1)
                       (.set search-params "size" 50)

                       (set-search-params! search-params))]

    ($ Card {:className "my-4"}
       ($ CardHeader {:className "flex sticky top-12 
                      bg-white rounded-md z-10"}
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
                           :variant "secondary"
                           :on-click handle-reset}
                   ($ ListRestart)))

             ($ :div {:className "flex space-x-2 mt-2"}
                (let [entries (.. search-params (entries))]
                  (for [[key value] entries]
                    ($ :<> {:key (str key value)}
                       (cond
                         (= key "owned")
                         ($ Badge {:key (str key value)
                                   :variant "secondary"
                                   :class-name "rounded-full"}
                            (str (t "pool.models.filters.status.owned")
                                 ": "
                                 (if (= value "true") (t "pool.models.filters.status.yes")
                                     (t "pool.models.filters.status.no"))))

                         (= key "in_stock")
                         ($ Badge {:key (str key value)
                                   :variant "secondary"
                                   :class-name "rounded-full"}
                            (str (t "pool.models.filters.status.in_stock")
                                 ": "
                                 (if (= value "true") (t "pool.models.filters.status.yes")
                                     (t "pool.models.filters.status.no"))))

                         (= key "incomplete")
                         ($ Badge {:key (str key value)
                                   :variant "secondary"
                                   :class-name "rounded-full"}
                            (str (t "pool.models.filters.status.incomplete")
                                 ": "
                                 (if (= value "true") (t "pool.models.filters.status.yes")
                                     (t "pool.models.filters.status.no"))))

                         (= key "broken")
                         ($ Badge {:key (str key value)
                                   :variant "secondary"
                                   :class-name "rounded-full"}
                            (str (t "pool.models.filters.status.broken")
                                 ": "
                                 (if (= value "true") (t "pool.models.filters.status.yes")
                                     (t "pool.models.filters.status.no"))))

                         (= key "before_last_check")
                         ($ Badge {:key (str key value)
                                   :variant "secondary"
                                   :class-name "rounded-full"}
                            (str (t "pool.models.filters.before_last_check.title")
                                 ": "
                                 (str value)))

                         (= key "category_id")
                         ($ Badge {:key (str key value)
                                   :variant "secondary"
                                   :class-name "rounded-full"}
                            (str (t "pool.models.filters.categories.title")
                                 ": "
                                 (find-category-name categories value)))

                         (= key "inventory_pool_id")
                         ($ Badge {:key (str key value)
                                   :variant "secondary"
                                   :class-name "rounded-full"}
                            (str (t "pool.models.filters.inventory_pool.title")
                                 ": "
                                 (:name (first (filter #(= (:id %) value) inventory-pools)))))

                         (= key "type")
                         ($ Badge {:key (str key value)
                                   :variant "secondary"
                                   :class-name "rounded-full"}
                            (str (t "pool.models.filters.type.title")
                                 ": "
                                 (t (str "pool.models.filters.type." value)))))))))))

       ($ pagination/main {:pagination pagination
                           :class-name "justify-start p-6"})

       ($ CardContent
          ($ :section {:className "rounded-md border"}

             (if (not (seq models))
               ($ :div {:className "flex p-6 justify-center"}
                  (t "pool.models.list.empty"))

               ($ Table
                  ($ TableHeader
                     ($ TableRow
                        ($ TableHead "")
                        ($ TableHead (t "pool.models.list.header.amount"))
                        ($ TableHead "")
                        ($ TableHead {:className "w-full"} (t "pool.models.list.header.name"))
                        ($ TableHead (t "pool.models.list.header.availability"))
                        ($ TableHead "")))
                  ($ TableBody

                     (for [model models]
                       ($ TableRow {:key (-> model :id)}
                          ($ TableCell
                             ($ Button {:variant "outline"
                                        :size "icon"} "+"))

                          ($ TableCell (-> model :total str))

                          ($ TableCell
                             ($ :div {:className "flex gap-2"}
                                ($ Image)
                                ($ Badge {:className (cond
                                                       (-> model :is_package)
                                                       "bg-lime-500"

                                                       (and (= (-> model :type) "Model")
                                                            (not (:is_package model)))
                                                       "bg-slate-500"

                                                       (= (-> model :type) "Option")
                                                       "bg-emerald-500"

                                                       (= (-> model :type) "Software")
                                                       "bg-orange-500")}

                                   (str (if (:is_package model)
                                          (t "pool.models.filters.type.package")
                                          (-> model :type))))))

                          ($ TableCell {:className "font-bold"}
                             (str (:product model) " " (:version model)))

                          ($ TableCell {:className "text-right"}
                             (str (-> model :available str) " | " (-> model :total str)))

                          ($ TableCell {:className "fit-content"}
                             ($ :div {:className "flex gap-2"}

                                ($ Button {:variant "outline"}
                                   ($ Link {:state #js {:searchParams (.. location -search)}
                                            :to (str (:id model))
                                            :viewTransition true}
                                      (t "pool.models.list.actions.edit")))

                                ($ DropdownMenu
                                   ($ DropdownMenuTrigger {:asChild "true"}
                                      ($ Button {:variant "secondary"
                                                 :size "icon"}
                                         ($ Ellipsis {:className "h-4 w-4"})))
                                   ($ DropdownMenuContent {:align "start"}
                                      ($ DropdownMenuItem
                                         ($ Link {:to (str (:id model) "/items/create")
                                                  :state #js {:searchParams (.. location -search)}
                                                  :viewTransition true}
                                            (t "pool.models.list.actions.add_item"))))))))))))))

       ($ pagination/main {:pagination pagination
                           :class-name "p-6 pt-0"}))))

