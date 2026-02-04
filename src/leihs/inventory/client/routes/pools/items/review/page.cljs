(ns leihs.inventory.client.routes.pools.items.review.page
  (:require
   ["@@/button" :refer [Button]]
   ["@@/card" :refer [Card CardContent]]
   ["@@/item" :refer [Item ItemContent ItemGroup ItemSeparator]]
   ["@@/label" :refer [Label]]
   ["@@/separator" :refer [Separator]]
   ["@@/switch" :refer [Switch]]
   ["@@/table" :refer [Table TableBody TableCell TableHead TableHeader
                       TableRow]]
   ["lucide-react" :refer [ArrowLeft Download]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [Link]]
   [leihs.inventory.client.components.barcode :refer [Barcode]]
   [leihs.inventory.client.components.export :refer [Export]]
   [leihs.inventory.client.components.typo :refer [Typo]]
   [leihs.inventory.client.routes.pools.items.review.components.serial-number :refer [SerialNumber]]
   [uix.core :as uix :refer [$ defui]]))

(defui page []
  (let [{:keys [data model]} (router/useLoaderData)
        params (router/useParams)
        location (router/useLocation)
        state (.. location -state)
        pool-id (aget params "pool-id")
        item-count (count data)

        [t] (useTranslation)

        [barcode? set-barcode!] (uix/use-state false)
        [urls? set-urls!] (uix/use-state false)

        handle-save (fn [index]
                      (let [next (inc index)
                            query-selector (if (< next item-count)
                                             (str "[data-sn-id='" (:id (nth data next)) "']")
                                             nil)
                            next-input (.. js/document (querySelector query-selector))]
                        (when next-input
                          (.focus next-input))))]

    ($ :div {:class-name "p-4"}
       ($ Typo {:class-name "my-8"
                :variant "h1"}
          (t "pool.items.review.title"))

       ($ Card {:class-name "py-8 mb-12"}
          ($ CardContent
             ($ Typo {:variant :h3}
                (t "pool.items.review.summary.title"))
             ($ ItemGroup {:class-name "w-full md:w-max mt-4"}

                ($ Item
                   ($ ItemContent {:class-name "flex md:flex-row"}
                      ($ :div {:class-name "w-32 text-muted-foreground"}
                         (t "pool.items.review.summary.count"))
                      ($ :div item-count)))
                ($ ItemSeparator)

                ($ Item
                   ($ ItemContent {:class-name "flex md:flex-row"}
                      ($ :div {:class-name "w-32 text-muted-foreground"}
                         (t "pool.items.review.summary.name"))
                      ($ Link {:to (str "/inventory/" pool-id "/models/" (:id model))}
                         ($ Typo {:variant :link}
                            (:product model)))))

                ($ ItemSeparator)

                ($ Item
                   ($ ItemContent {:class-name "flex md:flex-row"}
                      ($ :div {:class-name "w-32 text-muted-foreground"}
                         (t "pool.items.review.summary.date"))
                      ($ :div (t "intlDateTime" #js {:val (js/Date. (-> data first :created_at))}))))
                ($ ItemSeparator))

             ($ Export {:class-name "mt-6 shadow-md"
                        :url (str "/inventory/" pool-id "/items/")})

             ($ Separator {:class-name "my-8"})

             ($ Typo {:variant :h3}
                (t "pool.items.review.list_title"))

             ($ :div {:class-name "mt-4 space-y-2"}
                ($ :div {:class-name "flex items-center space-x-2"}
                   ($ Switch {:id "barcode"
                              :checked barcode?
                              :on-checked-change #(set-barcode! %)})
                   ($ Label {:for "barcode"}
                      (t "pool.items.review.toggles.show_barcodes")))

                ($ :div {:class-name "flex items-center space-x-2 "}
                   ($ Switch {:id "urls"
                              :checked urls?
                              :on-checked-change #(set-urls! %)})
                   ($ Label {:for "urls"}
                      (t "pool.items.review.toggles.show_urls"))))

             ($ :div {:class-name "rounded-lg border mt-6 w-full overflow-x-auto"}
                ($ Table {:class-name "table-fixed"}
                   ($ TableHeader
                      ($ TableRow

                         ($ TableHead {:class-name "w-[50px]"}
                            (t "pool.items.review.table.headers.number"))

                         ($ TableHead {:class-name "w-[220px]"}
                            (if barcode?
                              (t "pool.items.review.table.headers.barcode")
                              (t "pool.items.review.table.headers.inventory_code")))

                         ($ TableHead {:class-name "w-[300px]"}
                            (t "pool.items.review.table.headers.serial_number"))

                         ($ TableHead {:class-name "w-[500px]"}
                            (if urls?
                              (t "pool.items.review.table.headers.url")
                              (t "pool.items.review.table.headers.uuid")))))

                   ($ TableBody
                      (map-indexed (fn [idx item]
                                     (let [url (router/generatePath "/inventory/:pool-id/items/:item-id"
                                                                    #js {:pool-id pool-id
                                                                         :item-id (:id item)})]
                                       ($ TableRow {:key (str "item-" (:id item))}

                                          ($ TableCell {:class-name "text-muted-foreground"}
                                             (inc idx))

                                          ($ TableCell
                                             (if barcode?
                                               ($ Barcode {:value (:inventory_code item)
                                                           :font-size 12
                                                           :width 1.5
                                                           :height 30})
                                               (:inventory_code item)))

                                          ($ SerialNumber {:item item
                                                           :on-save #(handle-save idx)
                                                           :pool-id pool-id})

                                          ($ TableCell
                                             (if urls?
                                               ($ Link {:to url
                                                        :viewTransition true}
                                                  ($ Typo {:variant :link}
                                                     url))
                                               ($ Link {:to url
                                                        :viewTransition true}
                                                  ($ Typo {:variant :link}
                                                     (:id item))))))))
                                   data))))
             ($ Button {:class-name "mt-6"
                        :as-child true
                        :variant "outline"}
                ($ Link {:to (str "/inventory/" pool-id "/list"
                                  (some-> state .-searchParams))
                         :viewTransition true}
                   ($ ArrowLeft) (t "pool.items.review.back_to_inventory"))))))))

