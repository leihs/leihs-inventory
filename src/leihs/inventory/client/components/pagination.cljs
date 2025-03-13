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

        ;; derived values from simplified pagination map
        current-page (:page pagination)
        per-page (:size pagination)
        total-pages (:total_pages pagination)
        prev-page (when (> current-page 1) (dec current-page))
        next-page (when (< current-page total-pages) (inc current-page))

        gen-page-number-str (fn [number]
                              (.. search-params (set "page" number))
                              (.. search-params (toString)))

        gen-page-size-str (fn [value]
                            (.. search-params (set "size" value))
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

          ;; first page and ellipsis if current > 2
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

          ;; current page
             ($ PaginationItem
                ($ PaginationLink {:is-active true
                                   :href (str (.. location -pathname)
                                              "?"
                                              (gen-page-number-str current-page))}
                   current-page))

          ;; next page (number)
             (when (and (not= next-page total-pages)
                        next-page)
               ($ PaginationItem
                  ($ PaginationLink {:href (str (.. location -pathname)
                                                "?"
                                                (gen-page-number-str next-page))}
                     next-page)))

          ;; ellipsis if not on last page
             (when (not= current-page total-pages)
               ($ PaginationEllipsis))

          ;; last page if not current
             (when (not= current-page total-pages)
               ($ PaginationItem
                  ($ PaginationLink {:href (str (.. location -pathname)
                                                "?"
                                                (gen-page-number-str total-pages))}
                     total-pages)))

          ;; next link
             (if next-page
               ($ PaginationNext {:href (str (.. location -pathname)
                                             "?"
                                             (gen-page-number-str next-page))})
               ($ Button {:variant "link"
                          :disabled true}
                  "Next" ($ ChevronRight)))))

      ;; page size dropdown
       ($ DropdownMenu
          ($ DropdownMenuTrigger {:asChild "true"}
             ($ Button {:variant "outline"} "Items per page"))
          ($ DropdownMenuContent {:align "start"}
             ($ DropdownMenuRadioGroup {:value pages
                                        :onValueChange handle-size-change}
                ($ DropdownMenuRadioItem {:value 10} "10")
                ($ DropdownMenuRadioItem {:value 20} "20")
                ($ DropdownMenuRadioItem {:value 50} "50")
                ($ DropdownMenuRadioItem {:value 100} "100")))))))
