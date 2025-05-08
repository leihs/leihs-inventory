(ns leihs.inventory.client.routes.models.page
  (:require
   ["@@/badge" :refer [Badge]]
   ["@@/button" :refer [Button]]
   ["@@/calendar" :refer [Calendar]]
   ["@@/card" :refer [Card CardContent CardHeader]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuCheckboxItem
                               DropdownMenuContent DropdownMenuItem
                               DropdownMenuTrigger]]
   ["@@/input" :refer [Input]]
   ["@@/popover" :refer [Popover PopoverContent PopoverTrigger]]
   ["@@/select" :refer [Select SelectContent SelectItem SelectTrigger
                        SelectValue]]
   ["@@/table" :refer [Table TableBody TableCell TableHead TableHeader
                       TableRow]]
   ["date-fns" :as date-fns]
   ["lucide-react" :refer [CalendarDays Download Ellipsis Image Tags]]
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
                                     (set-search-params! search-params)))]

    ($ Card {:className "my-4"}
       ($ CardHeader {:className "flex sticky top-12 bg-white rounded-md z-10"}
          ($ :div
             ($ :div {:className "flex gap-2"}
                ($ Input {:placeholder "Suche Inventar"
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
                         ($ Tags {:className "h-4 w-4 mr-2"}) "Inventar-Typ"))
                   ($ DropdownMenuContent {:align "start"}
                      ($ DropdownMenuCheckboxItem
                         ($ Link "Model"))
                      ($ DropdownMenuCheckboxItem
                         ($ Link "Paket"))
                      ($ DropdownMenuCheckboxItem
                         ($ Link "Model"))
                      ($ DropdownMenuCheckboxItem
                         ($ Link "Model"))
                      ($ DropdownMenuCheckboxItem
                         ($ Link "Item"))))

                ($ Button {:variant "outline"}
                   ($ Tags {:className "h-4 w-4 mr-2"}) "Status")
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
                         (t "pool.models.filters.retired.note_retired"))))

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
                      ($ TableHead "Menge")
                      ($ TableHead "")
                      ($ TableHead {:className "w-full"} "Name")
                      ($ TableHead "Verfügbarkeit")
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
                              ($ Badge {:className (if (= (-> model :type) "Paket")
                                                     "bg-lime-500"
                                                     "bg-slate-600")}
                                 (str (-> model :type)))))

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
                                    "editieren"))

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
                                          "Gegenstand erstellen"))))))))))))

       ($ pagination/main {:pagination pagination
                           :class-name "p-6 pt-0"}))))

