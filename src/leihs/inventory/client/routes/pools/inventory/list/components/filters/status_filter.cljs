(ns leihs.inventory.client.routes.pools.inventory.list.components.filters.status-filter
  (:require
   ["@@/badge" :refer [Badge]]
   ["@@/button" :refer [Button]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent
                               DropdownMenuGroup DropdownMenuItem
                               DropdownMenuPortal DropdownMenuSub
                               DropdownMenuSubContent DropdownMenuSubTrigger
                               DropdownMenuTrigger]]
   ["@@/tooltip" :refer [TooltipProvider Tooltip TooltipContent TooltipTrigger]]
   ["lucide-react" :refer [Check ChevronDown CirclePlus]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [className]}]
  (let [[search-params set-search-params!] (router/useSearchParams)
        [t] (useTranslation)
        with_items (.. search-params (get "with_items"))
        type (.. search-params (get "type"))
        owned (.. search-params (get "owned"))
        in_stock (.. search-params (get "in_stock"))
        incomplete (.. search-params (get "incomplete"))
        broken (.. search-params (get "broken"))
        status-count (-> identity
                         (filter [owned in_stock incomplete broken])
                         count)
        handle-status (fn [e]
                        (let [name (.. e -target -name)
                              value (.. e -target -value)]

                          (cond
                            (= name "owned")
                            (cond
                              (= owned value)
                              (.delete search-params "owned")

                              (= value "true")
                              (.set search-params "owned" true)

                              (= value "false")
                              (.set search-params "owned" false))

                            (= name "in_stock")
                            (cond
                              (= in_stock value)
                              (.delete search-params "in_stock")

                              (= value "true")
                              (.set search-params "in_stock" true)

                              (= value "false")
                              (.set search-params "in_stock" false))

                            (= name "incomplete")
                            (cond
                              (= incomplete value)
                              (.delete search-params "incomplete")

                              (= value "true")
                              (.set search-params "incomplete" true)

                              (= value "false")
                              (.set search-params "incomplete" false))

                            (= name "broken")
                            (cond
                              (= broken value)
                              (.delete search-params "broken")

                              (= value "true")
                              (.set search-params "broken" true)

                              (= value "false")
                              (.set search-params "broken" false)))

                          (.set search-params "page" "1")
                          (set-search-params! search-params)))]

    ($ DropdownMenu
       ($ DropdownMenuTrigger {:asChild "true"}
          ($ Button {:variant "outline"
                     :disabled (or (= with_items "false")
                                   (= type "option"))
                     :class-name (str "min-w-48 max-w-48 " className)}

             ($ CirclePlus {:className "h-4 w-4"})
             (t "pool.models.filters.status.title")

             (if (pos? status-count)
               ($ Badge {:class-name "ml-auto w-6 justify-center "
                         :variant "secondary"}
                  status-count)
               ($ ChevronDown {:className "ml-auto h-4 w-4 opacity-50"}))))
       ($ DropdownMenuContent {:align "start"}
          ($ DropdownMenuGroup

             ($ DropdownMenuSub
                ($ DropdownMenuSubTrigger
                   ($ Button {:variant "ghost"
                              :class-name (str (when owned "italic ")
                                               "p-0 h-auto font-normal")
                              :type "button"}
                      (t "pool.models.filters.status.owned"))

                   ($ DropdownMenuPortal
                      ($ DropdownMenuSubContent

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
                   ($ Button {:variant "ghost"
                              :class-name (str (when in_stock "italic ")
                                               "p-0 h-auto font-normal")
                              :type "button"}
                      (t "pool.models.filters.status.in_stock"))

                   ($ DropdownMenuPortal
                      ($ DropdownMenuSubContent

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
                   ($ Button {:variant "ghost"
                              :class-name (str (when broken "italic ")
                                               "p-0 h-auto font-normal")
                              :disabled (= type "software")
                              :type "button"}
                      (t "pool.models.filters.status.broken"))

                   ($ DropdownMenuPortal
                      ($ DropdownMenuSubContent {:class-name (when (= type "software") "hidden ")}

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
                ($ DropdownMenuSubTrigger {:class-name (when incomplete "bg-accent")}
                   ($ Button {:variant "ghost"
                              :class-name (str (when incomplete "italic ")
                                               "p-0 h-auto font-normal")
                              :disabled (= type "software")
                              :type "button"}
                      (t "pool.models.filters.status.incomplete"))

                   ($ DropdownMenuPortal
                      ($ DropdownMenuSubContent

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
