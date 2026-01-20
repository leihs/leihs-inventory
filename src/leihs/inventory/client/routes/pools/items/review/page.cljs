(ns leihs.inventory.client.routes.pools.items.review.page

  (:require
   ["@@/button" :refer [Button]]
   ["@@/card" :refer [Card CardContent]]
   ["@@/item" :refer [Item ItemGroup ItemSeparator ItemTitle ItemDescription ItemContent]]
   ["@@/label" :refer [Label]]
   ["@@/separator" :refer [Separator]]
   ["@@/switch" :refer [Switch]]
   ["@@/table" :refer [Table TableBody TableCell TableHead TableHeader TableRow TableCaption]]
   ["@@/typo" :refer [Typo]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [Link]]

   [leihs.inventory.client.components.barcode :refer [Barcode]]
   [leihs.inventory.client.lib.utils :refer [cj]]
   [uix.core :as uix :refer [defui $]]))

(defui page [{:keys [item]}]
  (let [{:keys [data model]} (router/useLoaderData)
        params (router/useParams)
        pool-id (aget params "pool-id")
        item-count (count data)

        [t] (useTranslation)

        [barcode? set-barcode!] (uix/use-state false)
        [urls? set-urls!] (uix/use-state false)]

    ($ :div {:class-name "p-4"}
       ($ Typo {:class-name "my-8"
                :variant "h1"} "hello")

       ($ Card {:class-name "py-8 mb-12"}
          ($ CardContent
             ($ Typo {:variant "h3"} "Zusammenfassung")
             ($ ItemGroup {:class-name "w-max mt-4"}

                ($ Item
                   ($ ItemContent {:class-name "flex flex-row"}
                      ($ :div {:class-name "w-32 text-muted-foreground"} "Anzahl")
                      ($ :div item-count)))
                ($ ItemSeparator)

                ($ Item
                   ($ ItemContent {:class-name "flex flex-row"}
                      ($ :div {:class-name "w-32 text-muted-foreground"} "Name")
                      ($ Link {:to (str "/inventory/" pool-id "/models/" (:id model))}
                         ($ Typo {:variant "link"}
                            (:product model)))))

                ($ ItemSeparator)
                ($ Item
                   ($ ItemContent {:class-name "flex flex-row"}
                      ($ :div {:class-name "w-32 text-muted-foreground"} "Datum")
                      ($ :div (t "intlDateTime" #js {:val (js/Date. (-> data first :created_at))}))))
                ($ ItemSeparator))

             ($ Button {:class-name "mt-6 shadow-md"
                        :variant "outline"}
                "Export")

             ($ Separator {:class-name "my-8"})

             ($ Typo {:variant "h3"} "Details")

             ($ :div {:class-name "mt-4 space-y-2"}
                ($ :div {:class-name "flex items-center space-x-2"}
                   ($ Switch {:id "barcode"
                              :checked barcode?
                              :on-checked-change #(set-barcode! %)})
                   ($ Label {:for "barcode"} "Zeige Barcodes"))

                ($ :div {:class-name "flex items-center space-x-2 "}
                   ($ Switch {:id "urls"
                              :checked urls?
                              :on-checked-change #(set-urls! %)})
                   ($ Label {:for "urls"} "Zeige URLs")))

             ($ :div {:class-name "rounded-lg border mt-6"}
                ($ Table
                   ($ TableHeader
                      ($ TableRow
                         ($ TableHead "#")
                         (if barcode?
                           ($ TableHead "Barcode")
                           ($ TableHead "Inventory code"))
                         ($ TableHead "Serialnumber")
                         (if urls?
                           ($ TableHead "URL")
                           ($ TableHead "UUID"))))

                   ($ TableBody
                      (map-indexed (fn [idx item]
                                     (let [url (router/generatePath "/inventory/:pool-id/items/:item-id"
                                                                    #js {:pool-id pool-id
                                                                         :item-id (:id item)})]
                                       ($ TableRow {:key (str "item-" (:id item))}
                                          ($ TableCell (inc idx))
                                          (if barcode?
                                            ($ TableCell ($ Barcode {:value (:inventory_code item)
                                                                     :font-size 12
                                                                     :width 1
                                                                     :height 30}))
                                            ($ TableCell (:inventory_code item)))

                                          ($ TableCell (:id item))

                                          (if urls?
                                            ($ TableCell
                                               ($ Link {:to url
                                                        :viewTransition true}
                                                  ($ Typo {:variant "link"}
                                                     url)))
                                            ($ TableCell
                                               ($ Link {:to url
                                                        :viewTransition true}
                                                  ($ Typo {:variant "link"}
                                                     (:id item))))))))
                                   data))))
             ($ Button {:class-name "mt-6"
                        :variant "outline"}
                "Back to Inventory"))))))




