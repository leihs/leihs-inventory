(ns leihs.inventory.client.routes.models.page
  (:require
   ["@@/badge" :refer [Badge]]
   ["@@/button" :refer [Button]]
   ["@@/card" :refer [Card CardContent CardHeader]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent
                               DropdownMenuItem DropdownMenuTrigger]]
   ["@@/table" :refer [Table TableBody TableCell TableHead TableHeader
                       TableRow]]
   ["lucide-react" :refer [Download Ellipsis Image ListRestart Tags Tags]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [Link]]
   [goog.functions]
   [leihs.inventory.client.components.pagination :as pagination]
   [leihs.inventory.client.routes.models.components.before-last-check-filter :refer [BeforeLastCheckFilter]]
   [leihs.inventory.client.routes.models.components.borrowable-filter :refer [BorrowableFilter]]
   [leihs.inventory.client.routes.models.components.category-filter :refer [CategoryFilter]]
   [leihs.inventory.client.routes.models.components.filter-indicator :refer [FilterIndicator]]
   [leihs.inventory.client.routes.models.components.inventory-pool-filter :refer [InventoryPoolFilter]]
   [leihs.inventory.client.routes.models.components.reset :refer [FilterReset]]
   [leihs.inventory.client.routes.models.components.retired-filter :refer [RetiredFilter]]
   [leihs.inventory.client.routes.models.components.search-filter :as search :refer [SearchFilter]]
   [leihs.inventory.client.routes.models.components.status-filter :refer [StatusFilter]]
   [leihs.inventory.client.routes.models.components.type-filter :refer [TypeFilter]]
   [leihs.inventory.client.routes.models.components.with-items-filter :refer [WithItemsFilter]]
   [leihs.inventory.client.routes.models.filter-reducer :as filter-reducer]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui page [{:keys [data]}]
  (let [models (:data (:models (router/useLoaderData)))

        dispatch (filter-reducer/use-filter-dispatcher)
        state (filter-reducer/use-filter-state)

        [t] (useTranslation)
        location (router/useLocation)
        navigate (router/useNavigate)
        pagination (:pagination (:models (router/useLoaderData)))
        handle-reset (fn [e]
                       (js/console.debug "hello")
                       (dispatch {:filter "owned" :value nil :delete true}))]

    ($ Card {:className "my-4"}
       ($ CardHeader {:className "flex sticky top-12 
                      bg-white rounded-xl z-10"
                      :style {:background "linear-gradient(to bottom, white 90%, transparent 100%)"}}
          ($ filter-reducer/main
             ($ :div
                ($ :div {:className "flex flex-wrap gap-2"}
                   ($ SearchFilter)
                   ($ TypeFilter)
                   ($ StatusFilter)
                   ($ InventoryPoolFilter)
                   ($ CategoryFilter)
                   ($ BeforeLastCheckFilter)

                   ($ Button {:variant "outline" :className "lg:ml-auto"}
                      ($ Download {:className "h-4 w-4 mr-2"}) "Export"))

                ($ :div {:className "flex flex-wrap gap-2 mt-2"}
                   ($ RetiredFilter)
                   ($ WithItemsFilter)
                   ($ BorrowableFilter)
                   ($ FilterReset))

                ($ :div {:className "flex space-x-2 mt-2"}
                   ($ FilterIndicator))))

          ($ pagination/main {:pagination pagination
                              :class-name "justify-start pt-6"}))

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
                                                       (= (-> model :type) "Package")
                                                       "bg-lime-500"

                                                       (= (-> model :type) "Model")
                                                       "bg-slate-500"

                                                       (= (-> model :type) "Option")
                                                       "bg-emerald-500"

                                                       (= (-> model :type) "Software")
                                                       "bg-orange-500")}

                                   (str (cond
                                          (= (-> model :type) "Package")
                                          (t "pool.models.filters.type.package")

                                          (= (-> model :type) "Model")
                                          (t "pool.models.filters.type.model")

                                          (= (-> model :type) "Option")
                                          (t "pool.models.filters.type.option")

                                          (= (-> model :type) "Software")
                                          (t "pool.models.filters.type.software"))))))

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

