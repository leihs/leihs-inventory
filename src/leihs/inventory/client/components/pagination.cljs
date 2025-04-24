(ns leihs.inventory.client.components.pagination
  (:require
   ["@@/button" :refer [Button]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent
                               DropdownMenuRadioGroup DropdownMenuRadioItem
                               DropdownMenuTrigger]]
   ["@@/pagination" :refer [Pagination PaginationContent PaginationEllipsis
                            PaginationItem PaginationLink PaginationNext
                            PaginationPrevious]]
   ["lucide-react" :refer [ChevronDown ChevronLeft ChevronRight]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router]
   [uix.core :as uix :refer [$ defui]]))

(defn page-range [current size total-rows]
  (let [start (* (- current 1) size)
        end (+ start size)
        last-page-number (js/Math.ceil (/ total-rows size))]

    (if (= current last-page-number)
      {:start (+ start 1)
       :end total-rows}

      {:start (+ start 1)
       :end end})))

(defui main [{:keys [pagination class-name]}]
  (let [[t] (useTranslation)
        location (router/useLocation)
        [search-params set-search-params!] (router/useSearchParams)
        [size-change set-size-change!] (uix/use-state false)
        size (js/parseInt (or (.. search-params (get "size")) 10))
        total-pages (:total_pages pagination)
        total-rows (:total_rows pagination)
        current-page (:page pagination)
        page-range (page-range current-page size total-rows)

        next-page (if (not= current-page
                            total-pages)
                    (inc current-page)
                    nil)
        prev-page (if (not= current-page 1)
                    (dec current-page)
                    nil)

        gen-page-number-str (fn [number]
                              (.. search-params (set "page" number))
                              (.. search-params (toString)))

        gen-page-size-str (fn [value]
                            (.. search-params (set "size" value))
                            ;; need to reset page number since it is an object
                            (.. search-params (set "page" current-page))
                            (.. search-params (toString)))

        handle-size-change (fn [value]
                             (set-size-change! true)
                             (set-search-params! (gen-page-size-str value)))]

    (uix/use-effect
     (fn []
        ;; if page number is greater than total pages reset page number to last page
       (when (and (> current-page total-pages) size-change)
         (set-search-params! (gen-page-number-str total-pages))
         (set-size-change! false)))
     [total-pages current-page gen-page-number-str
      set-search-params! size-change])

    ($ :div {:class-name (str "flex " class-name)}
       ($ Pagination {:class-name "justify-start w-fit mx-0 pr-6"}

          ;; previous link
          (if prev-page
            ($ PaginationPrevious {:to (str (.. location -pathname)
                                            "?"
                                            (gen-page-number-str prev-page))}
               ($ :span (t "pagination.previous")))

            ($ Button {:variant "link"
                       :disabled true}
               ($ ChevronLeft) (t "pagination.previous")))

          ($ PaginationContent

             ;; first page when current page is greater than 2
             (when (> current-page 2)
               ($ :<>
                  ($ PaginationItem
                     ($ PaginationLink {:to (str (.. location -pathname)
                                                 "?"
                                                 (gen-page-number-str 1))}
                        "1"))
                  ($ PaginationEllipsis)))

             ;; previous link
             (when prev-page
               ($ PaginationItem
                  ($ PaginationLink {:to (str (.. location -pathname)
                                              "?"
                                              (gen-page-number-str prev-page))}
                     prev-page)))

             ;; current active page
             ($ PaginationItem
                ($ PaginationLink {:is-active true
                                   :to (str (.. location -pathname)
                                            "?"
                                            (gen-page-number-str current-page))}
                   current-page))

             ;; next page when not last page
             ;; ellipsis between next page and last page, when not last page
             (when (< current-page (- total-pages 1))
               ($ :<>
                  ($ PaginationItem
                     ($ PaginationLink {:to (str (.. location -pathname)
                                                 "?"
                                                 (gen-page-number-str next-page))}
                        next-page))
                  ($ PaginationEllipsis)))

             ;; last page
             (when (not= current-page
                         total-pages)
               ($ PaginationItem
                  ($ PaginationLink {:to (str (.. location -pathname)
                                              "?"
                                              (gen-page-number-str total-pages))}
                     total-pages))))

             ;; next link
          (if next-page
            ($ PaginationNext {:disabled (not next-page)
                               :to (str (.. location -pathname)
                                        "?"
                                        (gen-page-number-str next-page))}
               ($ :span (t "pagination.next")))

            ($ Button {:variant "link"
                       :disabled true}
               (t "pagination.next") ($ ChevronRight))))

       ($ :div {:class-name "flex items-center"}
          ($ :span {:class-name "text-muted-foreground text-sm mr-2"}
             (t "pagination.range" #js {:range (str (:start page-range) "-" (:end page-range))
                                        :total total-rows})))

       ($ :div {:class-name "flex items-center ml-auto"}

          ($ :span {:class-name "mr-2"}
             (t "pagination.per-page"))

          ($ DropdownMenu
             ($ DropdownMenuTrigger {:asChild "true"}
                ($ Button {:variant "outline"} size ($ ChevronDown {:class-name "ml-1 h-4 w-4"})))

             ($ DropdownMenuContent {:align "start"}
                ($ DropdownMenuRadioGroup {:value size
                                           :onValueChange handle-size-change}
                   ($ DropdownMenuRadioItem {:value 10} "10")
                   ($ DropdownMenuRadioItem {:value 20} "20")
                   ($ DropdownMenuRadioItem {:value 50} "50")
                   ($ DropdownMenuRadioItem {:value 100} "100"))))))))
