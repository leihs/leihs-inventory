(ns leihs.inventory.client.components.pagination
  (:require
   ["@@/button" :refer [Button]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent
                               DropdownMenuRadioGroup DropdownMenuRadioItem
                               DropdownMenuTrigger]]
   ["@@/pagination" :refer [Pagination PaginationContent PaginationEllipsis
                            PaginationItem PaginationLink PaginationNext
                            PaginationPrevious]]
   ["lucide-react" :refer [ChevronLeft ChevronRight]]
   ["react-router-dom" :as router]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [pagination class-name]}]
  (let [location (router/useLocation)
        navigate (router/useNavigate)
        search-params (js/URLSearchParams. (.-search location))
        [pages set-pages!] (uix/use-state (js/parseInt (.. search-params (get "size"))))

        gen-page-number-str (fn [number]
                              (.. search-params (set "page" number))
                              (.. search-params (toString)))

        gen-page-size-str (fn [value]
                            (.. search-params (set "size" value))
                            (.. search-params (set "page" (:current_page pagination)))
                            (.. search-params (toString)))

        handle-size-change (fn [value]
                             (let [route (str (.. location -pathname)
                                              "?"
                                              (gen-page-size-str value))]
                               (set-pages! value)
                               (navigate route)))]

    ($ :div {:class-name (str "flex " class-name)}
       ($ Pagination {:class-name "justify-start"}

          ;; previous link
          (if (:prev_page pagination)
            ($ PaginationPrevious {:href (str (.. location -pathname)
                                              "?"
                                              (gen-page-number-str (:prev_page pagination)))})
            ($ Button {:variant "link"
                       :disabled true}
               ($ ChevronLeft) "Previous"))

          ($ PaginationContent

             ;; first page when current page is greater than 2
             (when (> (:current_page pagination) 2)
               ($ :<>
                  ($ PaginationItem
                     ($ PaginationLink {:href (str (.. location -pathname)
                                                   "?"
                                                   (gen-page-number-str 1))}
                        "1"))
                  ($ PaginationEllipsis)))

             ;; previous link
             (when (:prev_page pagination)
               ($ PaginationItem
                  ($ PaginationLink {:href (str (.. location -pathname)
                                                "?"
                                                (gen-page-number-str (:prev_page pagination)))}
                     (:prev_page pagination))))

             ;; current active page
             ($ PaginationItem
                ($ PaginationLink {:is-active true
                                   :href (str (.. location -pathname)
                                              "?"
                                              (gen-page-number-str (:current_page pagination)))}
                   (:current_page pagination)))

             ;; next page when not last page
             (when (and (not= (:next_page pagination)
                              (:total_pages pagination))
                        (:next_page pagination))
               ($ PaginationItem
                  ($ PaginationLink {:href (str (.. location -pathname)
                                                "?"
                                                (gen-page-number-str (:next_page pagination)))}
                     (:next_page pagination))))

             ;; ellipsis between next page and last page, when not last page
             (when (not= (:current_page pagination)
                         (:total_pages pagination))
               ($ PaginationEllipsis))

             ;; last page
             (when (not= (:current_page pagination)
                         (:total_pages pagination))
               ($ PaginationItem
                  ($ PaginationLink {:href (str (.. location -pathname)
                                                "?"
                                                (gen-page-number-str (:total_pages pagination)))}
                     (:total_pages pagination))))

             ;; next link
             (if (:next_page pagination)
               ($ PaginationNext {:disabled (not (:next_page pagination))
                                  :href (str (.. location -pathname)
                                             "?"
                                             (gen-page-number-str (:next_page pagination)))})
               ($ Button {:variant "link"
                          :disabled true}
                  "Next" ($ ChevronRight)))))

       ($ DropdownMenu
          ($ DropdownMenuTrigger {:asChild "true"}
             ($ Button {:variant "outline"} "Items per page "))
          ($ DropdownMenuContent {:align "start"}
             ($ DropdownMenuRadioGroup {:value pages
                                        :onValueChange handle-size-change}
                ($ DropdownMenuRadioItem {:value 10} "10")
                ($ DropdownMenuRadioItem {:value 20} "20")
                ($ DropdownMenuRadioItem {:value 50} "50")
                ($ DropdownMenuRadioItem {:value 100} "100")))))))
