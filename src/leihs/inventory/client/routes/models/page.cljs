(ns leihs.inventory.client.routes.models.page
  (:require
   ["@@/badge" :refer [Badge]]
   ["@@/button" :refer [Button]]
   ["@@/calendar" :refer [Calendar]]
   ["@@/card" :refer [Card CardContent CardHeader]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuCheckboxItem
                               DropdownMenuContent DropdownMenuItem
                               DropdownMenuTrigger DropdownMenuRadioGroup
                               DropdownMenuRadioItem]]
   ["@@/input" :refer [Input]]
   ["@@/popover" :refer [Popover PopoverContent PopoverTrigger]]
   ["@@/select" :refer [Select SelectContent SelectItem SelectTrigger
                        SelectValue]]
   ["@@/table" :refer [Table TableBody TableCell TableHead TableHeader
                       TableRow]]
   ["date-fns" :as date-fns]
   ["lucide-react" :refer [CalendarDays Download Ellipsis Image Tags CirclePlus]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [Link]]
   [goog.functions]
   [leihs.inventory.client.components.pagination :as pagination]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui page [{:keys [data]}]
  (let [models (:data (router/useLoaderData))
        [t] (useTranslation)
        location (router/useLocation)
        [search-params set-search-params!] (router/useSearchParams)
        pagination (:pagination (router/useLoaderData))

        retired (js/JSON.parse (.. search-params (get "retired")))
        handle-retired (fn [e]
                         (if (= e nil)
                           (.delete search-params "retired")
                           (.set search-params "retired" e))
                         (set-search-params! search-params))

        with_items (js/JSON.parse (.. search-params (get "with_items")))
        handle-with-items (fn [e]
                            (if (= e nil)
                              (.delete search-params "with_items")
                              (.set search-params "with_items" e))
                            (set-search-params! search-params))

        borrowable (js/JSON.parse (.. search-params (get "borrowable")))
        handle-borrowable (fn [e]
                            (if (= e nil)
                              (.delete search-params "borrowable")
                              (.set search-params "borrowable" e))
                            (set-search-params! search-params))

        before-last-check (.. search-params (get "before_last_check"))
        handle-before-last-check (fn [date]
                                   (let [formatted-date (date-fns/format date "yyyy-MM-dd")]
                                     (if (= date nil)
                                       (.delete search-params "before_last_check")
                                       (.set search-params "before_last_check" formatted-date))
                                     (set-search-params! search-params)))

        handle-type (fn [e]
                      (let [current-type (.. search-params (get "type"))]
                        (if (or (= e nil) (= e current-type))
                          (.delete search-params "type")
                          (.set search-params "type" e))
                        (set-search-params! search-params)))

        status (let [owned (.. search-params (get "owned"))
                     in_stock (.. search-params (get "in_stock"))
                     incomplete (.. search-params (get "incomplete"))
                     broken (.. search-params (get "broken"))]
                 (cond
                   owned ["owned" (.. search-params (get "owned"))]
                   in_stock ["in_stock" (.. search-params (get "in_stock"))]
                   incomplete ["incomplete" (.. search-params (get "incomplete"))]
                   broken ["broken" (.. search-params (get "broken"))]))

        handle-status (fn [e]
                        (let [status (.. search-params (get e))]
                          (js/console.debug e status search-params)

                          (.delete search-params "in_stock")
                          (.delete search-params "owned")
                          (.delete search-params "incomplete")
                          (.delete search-params "broken")

                          (js/console.debug "after delete")
                          (.set search-params e true)))]

    ($ Card {:className "my-4"}
       ($ CardHeader {:className "flex sticky top-12 bg-white rounded-md z-10"}
          ($ :div
             ($ :div {:className "flex gap-2"}
                ($ Input {:placeholder (t "pool.models.filters.search.placeholder")
                          :name "search"
                          :className "w-fit py-0"
                          :onChange (fn [e]
                                      (let [value (.. e -target -value)]
                                        (if (= value "")
                                          (.delete search-params "search")
                                          (.set search-params "search" value))
                                        (set-search-params! search-params)))})

                ($ DropdownMenu
                   ($ DropdownMenuTrigger {:asChild "true"}
                      ($ Button {:variant "outline"}
                         ($ Tags {:className "h-4 w-4 mr-2"}) (t "pool.models.filters.type.title")))
                   ($ DropdownMenuContent {:align "start"}
                      ($ DropdownMenuRadioGroup {:value (.. search-params (get "type"))
                                                 :onValueChange handle-type}
                         ($ DropdownMenuRadioItem {:value "model"}
                            ($ Badge {:class-name "bg-slate-500 hover:bg-slate-500"}
                               (t "pool.models.filters.type.model")))

                         ($ DropdownMenuRadioItem {:value "package"}
                            ($ Badge {:class-name "bg-lime-500 hover:bg-lime-500"}
                               (t "pool.models.filters.type.package")))

                         ($ DropdownMenuRadioItem {:value "option"}
                            ($ Badge {:class-name "bg-emerald-500 hover:bg-emerald-500"}
                               (t "pool.models.filters.type.option")))

                         ($ DropdownMenuRadioItem {:value "software"}
                            ($ Badge {:class-name "bg-orange-500 hover:bg-orange-500"}
                               (t "pool.models.filters.type.software"))))))

                ($ DropdownMenu
                   ($ DropdownMenuTrigger {:asChild "true"}
                      ($ Button {:variant "outline"}
                         ($ CirclePlus {:className "h-4 w-4 mr-2"}) (t "pool.models.filters.status.title")))
                   ($ DropdownMenuContent {:align "start"}
                      ($ DropdownMenuRadioGroup {:value (.. search-params (get "type"))
                                                 :onValueChange handle-status}
                         ($ DropdownMenuRadioItem {:value "owned"}
                            (t "pool.models.filters.status.owned"))

                         ($ DropdownMenuRadioItem {:value "in_stock"}
                            (t "pool.models.filters.status.in_stock"))

                         ($ DropdownMenuRadioItem {:value "incomplete"}
                            (t "pool.models.filters.status.incomplete"))

                         ($ DropdownMenuRadioItem {:value "broken"}
                            (t "pool.models.filters.status.broken")))))

                ($ Button {:variant "outline"}
                   ($ Tags {:className "h-4 w-4 mr-2"}) "Geraetepark")
                ($ Button {:variant "outline"}
                   ($ Tags {:className "h-4 w-4 mr-2"}) "Kategorien")

                ($ Popover
                   ($ PopoverTrigger {:asChild true}
                      ($ Button {:variant "outline"}
                         ($ CalendarDays {:className "h-4 w-4 mr-2"}) "Inventur vor"))

                   ($ PopoverContent {:className "w-[280px]"}
                      ($ Calendar {:mode "single"
                                   :selected before-last-check
                                   :onSelect handle-before-last-check})))

                ($ Button {:variant "outline" :className "ml-auto"}
                   ($ Download {:className "h-4 w-4 mr-2"}) "Export"))

             ($ :div {:className "flex gap-2 mt-2"}
                ($ Select {:value retired
                           :onValueChange handle-retired}
                   ($ SelectTrigger {:name "retired"
                                     :className "w-[240px]"}
                      ($ SelectValue))
                   ($ SelectContent
                      ($ SelectItem {:data-test-id "all"
                                     :value nil}
                         (t "pool.models.filters.retired.all"))
                      ($ SelectItem {:data-test-id "retired"
                                     :value true}
                         (t "pool.models.filters.retired.retired"))
                      ($ SelectItem {:data-test-id "not_retired"
                                     :value false}
                         (t "pool.models.filters.retired.not_retired"))))

                ($ Select {:value with_items
                           :onValueChange handle-with-items}
                   ($ SelectTrigger {:name "with_items"
                                     :className "w-[240px]"}
                      ($ SelectValue))
                   ($ SelectContent
                      ($ SelectItem {:data-test-id "all"
                                     :value nil}
                         (t "pool.models.filters.with_items.all"))
                      ($ SelectItem {:data-test-id "with_items"
                                     :value true}
                         (t "pool.models.filters.with_items.with_items"))
                      ($ SelectItem {:data-test-id "without_items"
                                     :value false}
                         (t "pool.models.filters.with_items.without_items"))))

                ($ Select {:value borrowable
                           :onValueChange handle-borrowable}
                   ($ SelectTrigger {:name "borrowable"
                                     :className "w-[240px]"}
                      ($ SelectValue))
                   ($ SelectContent
                      ($ SelectItem {:data-test-id "all"
                                     :value nil}
                         (t "pool.models.filters.borrowable.all"))
                      ($ SelectItem {:data-test-id "borrowable"
                                     :value true}
                         (t "pool.models.filters.borrowable.borrowable"))
                      ($ SelectItem {:data-test-id "not_borrowable"
                                     :value false}
                         (t "pool.models.filters.borrowable.not_borrowable")))))))

       ($ pagination/main {:pagination pagination
                           :class-name "justify-start p-6"})

       ($ CardContent
          ($ :section {:className "rounded-md border"}
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

                                                     #_(= (-> model :type) "Item")
                                                     #_"bg-blue-500"

                                                     (= (-> model :type) "Option")
                                                     "bg-emerald-500"

                                                     (= (-> model :type) "Software")
                                                     "bg-orange-500"

                                                     #_(= (-> model :type) "Software-License")
                                                     #_"bg-pink-500")}

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
                                          (t "pool.models.list.actions.add_item")))))))))))))

       ($ pagination/main {:pagination pagination
                           :class-name "p-6 pt-0"}))))

