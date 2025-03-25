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
        size (js/parseInt (.. search-params (get "size")))
        total-pages (:total_pages pagination)
        current-page (:page pagination)

        next-page (if (not= current-page
                            total-pages)
                    (inc current-page)
                    nil)
        prev-page (if (not= current-page 1)
                    (dec current-page)
                    nil)
        [pages set-pages!] (uix/use-state (if (js/isNaN size) 10 size))

        gen-page-number-str (fn [number]
                              (.. search-params (set "page" number))
                              (.. search-params (toString)))

        gen-page-size-str (fn [value]
                            (.. search-params (set "size" value))
                            ;; need to reset page number since it is an object
                            (.. search-params (set "page" current-page))
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
          (if prev-page
            ($ PaginationPrevious {:href (str (.. location -pathname)
                                              "?"
                                              (gen-page-number-str prev-page))})
            ($ Button {:variant "link"
                       :disabled true}
               ($ ChevronLeft) "Previous"))

          ($ PaginationContent

             ;; first page when current page is greater than 2
             (when (> current-page 2)
               ($ :<>
                  ($ PaginationItem
                     ($ PaginationLink {:href (str (.. location -pathname)
                                                   "?"
                                                   (gen-page-number-str 1))}
                        "1"))
                  ($ PaginationEllipsis)))

             ;; previous link
             (when prev-page
               ($ PaginationItem
                  ($ PaginationLink {:href (str (.. location -pathname)
                                                "?"
                                                (gen-page-number-str prev-page))}
                     prev-page)))

             ;; current active page
             ($ PaginationItem
                ($ PaginationLink {:is-active true
                                   :href (str (.. location -pathname)
                                              "?"
                                              (gen-page-number-str current-page))}
                   current-page))

             ;; next page when not last page
             (when (and (not= next-page
                              total-pages)
                        next-page)
               ($ PaginationItem
                  ($ PaginationLink {:href (str (.. location -pathname)
                                                "?"
                                                (gen-page-number-str next-page))}
                     next-page)))

             ;; ellipsis between next page and last page, when not last page
             (when (not= current-page
                         total-pages)
               ($ PaginationEllipsis))

             ;; last page
             (when (not= current-page
                         total-pages)
               ($ PaginationItem
                  ($ PaginationLink {:href (str (.. location -pathname)
                                                "?"
                                                (gen-page-number-str total-pages))}
                     total-pages)))

             ;; next link
             (if next-page
               ($ PaginationNext {:disabled (not next-page)
                                  :href (str (.. location -pathname)
                                             "?"
                                             (gen-page-number-str next-page))})
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
