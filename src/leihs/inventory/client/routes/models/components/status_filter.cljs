(ns leihs.inventory.client.routes.models.components.status-filter
  (:require
   ["@@/button" :refer [Button]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent
                               DropdownMenuGroup DropdownMenuItem
                               DropdownMenuPortal DropdownMenuSub
                               DropdownMenuSubContent DropdownMenuSubTrigger
                               DropdownMenuTrigger]]
   ["lucide-react" :refer [Check ChevronDown CirclePlus]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router]
   [leihs.inventory.client.routes.models.filter-reducer :as filter-reducer]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [class-name]}]
  (let [[search-params set-search-params!] (router/useSearchParams)

        dispatch (filter-reducer/use-filter-dispatcher)
        state (filter-reducer/use-filter-state)

        [t] (useTranslation)
        with_items (.. search-params (get "with_items"))
        owned (.. search-params (get "owned"))
        in_stock (.. search-params (get "in_stock"))
        incomplete (.. search-params (get "incomplete"))
        broken (.. search-params (get "broken"))
        handle-status (fn [e]
                        (let [name (.. e -target -name)
                              value (.. e -target -value)]

                          (cond
                            (= name "owned")
                            (cond
                              (= owned value)
                              (dispatch {:filter "owned" :value nil :delete true})

                              (= value "true")
                              (dispatch {:filter "owned" :value true})

                              (= value "false")
                              (dispatch {:filter "owned" :value false}))

                            (= name "in_stock")
                            (cond
                              (= in_stock value)
                              (dispatch {:filter "in_stock" :value nil :delete true})

                              (= value "true")
                              (dispatch {:filter "in_stock" :value true})

                              (= value "false")
                              (dispatch {:filter "in_stock" :value false}))

                            (= name "incomplete")
                            (cond
                              (= incomplete value)
                              (dispatch {:filter "incomplete" :value nil :delete true})

                              (= value "true")
                              (dispatch {:filter "incomplete" :value true})

                              (= value "false")
                              (dispatch {:filter "incomplete" :value false}))

                            (= name "broken")
                            (cond
                              (= broken value)
                              (dispatch {:filter "broken" :value nil :delete true})

                              (= value "true")
                              (dispatch {:filter "broken" :value true})

                              (= value "false")
                              (dispatch {:filter "broken" :value false})))))]

    ($ DropdownMenu
       ($ DropdownMenuTrigger {:asChild "true"}
          ($ Button {:variant "outline"
                     :disabled (some #{:status} state)
                     :name "status-filter"
                     :class-name class-name}
             ($ CirclePlus {:className "h-4 w-4 mr-2 "})
             (t "pool.models.filters.status.title")
             ($ ChevronDown {:className "ml-auto h-4 w-4 opacity-50"})))
       ($ DropdownMenuContent {:align "start"}
          ($ DropdownMenuGroup

             ($ DropdownMenuSub
                ($ DropdownMenuSubTrigger
                   ($ Button {:type "button"
                              :class-name "p-0 h-auto font-normal"
                              :variant "ghost"
                              :disabled (some #{:owned} state)}
                      (t "pool.models.filters.status.owned"))

                   ($ DropdownMenuPortal
                      ($ DropdownMenuSubContent {:class-name (when (some #{:owned} state) "hidden")}

                         ($ DropdownMenuItem {:asChild true}
                            ($ Button {:variant "ghost"
                                       :name "owned"
                                       :value true
                                       :on-click handle-status
                                       :class-name "w-full justify-start font-normal"}
                               (t "pool.models.filters.status.yes")
                               (when (= owned "true")
                                 ($ Check {:className "ml-auto h-4 w-4"}))))

                         ($ DropdownMenuItem {:asChild true}
                            ($ DropdownMenuItem {:asChild true}
                               ($ Button {:variant "ghost"
                                          :name "owned"
                                          :value false
                                          :on-click handle-status
                                          :class-name "w-full justify-start font-normal"}
                                  (t "pool.models.filters.status.no")
                                  (when (= owned "false")
                                    ($ Check {:className "ml-auto h-4 w-4"})))))))))

             ($ DropdownMenuSub
                ($ DropdownMenuSubTrigger
                   ($ Button {:type "button"
                              :class-name "p-0 h-auto font-normal"
                              :variant "ghost"
                              :disabled (some #{:in_stock} state)}
                      (t "pool.models.filters.status.in_stock"))

                   ($ DropdownMenuPortal
                      ($ DropdownMenuSubContent {:class-name (when (some #{:in_stock} state) "hidden")}

                         ($ DropdownMenuItem {:asChild true}
                            ($ Button {:variant "ghost"
                                       :name "in_stock"
                                       :value true
                                       :on-click handle-status
                                       :class-name "w-full justify-start font-normal"}
                               (t "pool.models.filters.status.yes")
                               (when (= in_stock "true")
                                 ($ Check {:className "ml-auto h-4 w-4"}))))

                         ($ DropdownMenuItem {:asChild true}
                            ($ DropdownMenuItem {:asChild true}
                               ($ Button {:variant "ghost"
                                          :name "in_stock"
                                          :value false
                                          :on-click handle-status
                                          :class-name "w-full justify-start font-normal"}
                                  (t "pool.models.filters.status.no")
                                  (when (= in_stock "false")
                                    ($ Check {:className "ml-auto h-4 w-4"})))))))))

             ($ DropdownMenuSub
                ($ DropdownMenuSubTrigger
                   ($ Button {:type "button"
                              :class-name "p-0 h-auto font-normal"
                              :variant "ghost"
                              :disabled (some #{:broken} state)
                              :name "broken-filter"}
                      (t "pool.models.filters.status.broken"))

                   ($ DropdownMenuPortal
                      ($ DropdownMenuSubContent {:class-name (when (some #{:broken} state) "hidden")}

                         ($ DropdownMenuItem {:asChild true}
                            ($ Button {:variant "ghost"
                                       :name "broken"
                                       :value true
                                       :on-click handle-status
                                       :class-name "w-full justify-start font-normal"}
                               (t "pool.models.filters.status.yes")
                               (when (= broken "true")
                                 ($ Check {:className "ml-auto h-4 w-4"}))))

                         ($ DropdownMenuItem {:asChild true}
                            ($ DropdownMenuItem {:asChild true}
                               ($ Button {:variant "ghost"
                                          :name "broken"
                                          :value false
                                          :on-click handle-status
                                          :class-name "w-full justify-start font-normal"}
                                  (t "pool.models.filters.status.no")
                                  (when (= broken "false")
                                    ($ Check {:className "ml-auto h-4 w-4"})))))))))

             ($ DropdownMenuSub
                ($ DropdownMenuSubTrigger
                   ($ Button {:type "button"
                              :variant "ghost"
                              :class-name "p-0 h-auto font-normal"
                              :disabled (some #{:broken} state)
                              :name "incomplete-filter"}
                      (t "pool.models.filters.status.incomplete"))

                   ($ DropdownMenuPortal
                      ($ DropdownMenuSubContent {:class-name (when (some #{:incomplete} state) "hidden")}

                         ($ DropdownMenuItem {:asChild true}
                            ($ Button {:variant "ghost"
                                       :name "incomplete"
                                       :value true
                                       :on-click handle-status
                                       :class-name "w-full justify-start font-normal"}
                               (t "pool.models.filters.status.yes")
                               (when (= incomplete "true")
                                 ($ Check {:className "ml-auto h-4 w-4"}))))

                         ($ DropdownMenuItem {:asChild true}
                            ($ DropdownMenuItem {:asChild true}
                               ($ Button {:variant "ghost"
                                          :name "incomplete"
                                          :value false
                                          :on-click handle-status
                                          :class-name "w-full justify-start font-normal"}
                                  (t "pool.models.filters.status.no")
                                  (when (= incomplete "false")
                                    ($ Check {:className "ml-auto h-4 w-4"}))))))))))))))

(def StatusFilter
  (uix/as-react
   (fn [props]
     (main props))))
