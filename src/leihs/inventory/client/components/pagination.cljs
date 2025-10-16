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
  (let [ref-next (uix/use-ref nil)
        ref-prev (uix/use-ref nil)
        [t] (useTranslation)
        location (router/useLocation)
        [search-params set-search-params!] (router/useSearchParams)
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

        gen-page-str (fn [number]
                       (.. search-params (set "page" number))
                       (.. search-params (toString)))

        handle-size-change (fn [value]
                             (.. search-params (set "size" value))
                             (.. search-params (set "page" 1))
                             (set-search-params! search-params))]

    (uix/use-effect
     (fn []
       (js/window.scrollTo #js {:top 0 :behavior "smooth"}))
     [current-page])

    (uix/use-effect
     (fn []
       (let [on-key-down
             (fn [e]
               (when (and (= (.. e -code) "ArrowRight")
                          (.-altKey e)
                          (.-shiftKey e)
                          (not (.-ctrlKey e))
                          (not (.-metaKey e)))
                 (.preventDefault e)
                 (when ref-next
                   (when-let [input-element (.-current ref-next)]
                     (.. input-element (click)))))

               (when (and (= (.. e -code) "ArrowLeft")
                          (.-altKey e)
                          (.-shiftKey e)
                          (not (.-ctrlKey e))
                          (not (.-metaKey e)))
                 (.preventDefault e)
                 (when ref-prev
                   (when-let [input-element (.-current ref-prev)]
                     (.. input-element (click))))))]

         (js/window.addEventListener "keydown" on-key-down)
         (fn [] (js/window.removeEventListener "keydown" on-key-down))))
     [])

    ($ :div {:data-test-id "pagination-container"
             :class-name (str "flex " class-name)}
       ($ Pagination {:class-name "overflow-hidden justify-start w-fit mx-0 pr-6"
                      :data-test-id "pagination"}

          ;; previous link
          (if prev-page
            ($ PaginationPrevious {:ref ref-prev
                                   :data-test-id "pagination-previous"
                                   :to (str (.. location -pathname)
                                            "?"
                                            (gen-page-str prev-page))}
               ($ :span (t "pagination.previous")))

            ($ Button {:data-test-id "pagination-previous"
                       :variant "link"
                       :disabled true}
               ($ ChevronLeft) (t "pagination.previous")))

          ($ PaginationContent

             ;; first page when current page is greater than 2
             (when (> current-page 2)
               ($ :<>
                  ($ PaginationItem
                     ($ PaginationLink {:data-test-id "pagination-first-page"
                                        :to (str (.. location -pathname)
                                                 "?"
                                                 (gen-page-str 1))}
                        "1"))
                  ($ PaginationEllipsis)))

             ;; previous link
             (when prev-page
               ($ PaginationItem
                  ($ PaginationLink {:data-test-id "pagination-previous-page"
                                     :to (str (.. location -pathname)
                                              "?"
                                              (gen-page-str prev-page))}
                     prev-page)))

             ;; current active page
             ($ PaginationItem
                ($ PaginationLink {:data-test-id "pagination-current-page"
                                   :is-active true
                                   :to (str (.. location -pathname)
                                            "?"
                                            (gen-page-str current-page))}
                   current-page))

             ;; next page when not last page
             ;; ellipsis between next page and last page, when not last page
             (when (< current-page (- total-pages 1))
               ($ :<>
                  ($ PaginationItem
                     ($ PaginationLink {:data-test-id "pagination-next-page"
                                        :to (str (.. location -pathname)
                                                 "?"
                                                 (gen-page-str next-page))}
                        next-page))
                  ($ PaginationEllipsis)))

             ;; last page
             (when (and (not= current-page
                              total-pages)
                        (> total-rows 0))
               ($ PaginationItem
                  ($ PaginationLink {:data-test-id "pagination-last-page"
                                     :to (str (.. location -pathname)
                                              "?"
                                              (gen-page-str total-pages))}
                     total-pages))))

          ;; next link
          (if (and next-page
                   (> total-rows 0))
            ($ PaginationNext {:data-test-id "pagination-next"
                               :ref ref-next
                               :disabled (not next-page)
                               :to (str (.. location -pathname)
                                        "?"
                                        (gen-page-str next-page))}
               ($ :span (t "pagination.next")))

            ($ Button {:data-test-id "pagination-next"
                       :variant "link"
                       :disabled true}
               (t "pagination.next") ($ ChevronRight))))

       ($ :div {:class-name "flex items-center"}
          ($ :span {:data-test-id "pagination-range"
                    :class-name "text-muted-foreground text-sm mr-2"}
             (t "pagination.range" #js {:range (str (:start page-range) "-" (:end page-range))
                                        :total total-rows})))

       ($ :div {:class-name "flex items-center ml-auto"}

          ($ :span {:class-name "mr-2"}
             (t "pagination.per-page"))

          ($ DropdownMenu
             ($ DropdownMenuTrigger {:asChild "true"}
                ($ Button {:data-test-id "pagination-size-button"
                           :variant "outline"}
                   size ($ ChevronDown {:class-name "ml-1 h-4 w-4"})))

             ($ DropdownMenuContent {:data-test-id "pagination-size-dropdown"
                                     :align "start"}
                ($ DropdownMenuRadioGroup {:value size
                                           :onValueChange handle-size-change}
                   ($ DropdownMenuRadioItem {:value 10}
                      ($ :button {:type "button"} "10"))
                   ($ DropdownMenuRadioItem {:value 20}
                      ($ :button {:type "button"} "20"))
                   ($ DropdownMenuRadioItem {:value 50}
                      ($ :button {:type "button"} "50"))
                   ($ DropdownMenuRadioItem {:value 100}
                      ($ :button {:type "button"} "100")))))))))
