(ns leihs.inventory.client.routes.pools.inventory.list.components.table.item-row
  (:require
   ["@@/badge" :refer [Badge]]
   ["@@/button" :refer [Button]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent
                               DropdownMenuItem DropdownMenuTrigger]]
   ["@@/table" :refer [TableCell TableRow]]
   ["date-fns" :refer [format]]
   ["lucide-react" :refer [Ellipsis Image]]
   ["react-i18next" :refer [useTranslation]]

   ["react-router-dom" :as router :refer [Link]]
   [leihs.inventory.client.routes.pools.inventory.list.components.table.item-status :refer [ItemStatus]]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [item isPackageItem]
              :or {isPackageItem false}}]
  (let [location (router/useLocation)
        [t] (useTranslation)
        params (router/useParams)]

    ($ TableRow {:key (-> item :id)
                 :class-name "bg-destructive-foreground/50"}

       ($ TableCell)

       ($ TableCell {:className "text-right"}
          (if (:url item)
            ($ :img {:class-name "w-12 h-12 object-contain inline"
                     :src (:url item)})
            ($ Image {:class-name "w-7 h-7 inline"})))

       ($ TableCell
          ($ :div {:className "flex gap-2 "}
             ($ Badge {:className "w-6 h-6 justify-center bg-blue-500"} "G")))

       ($ TableCell
          ($ :div {:class-name "flex flex-row items-center"}
             ($ :span {:class-name "w-32"}
                (:inventory_code item))
             ($ :div {:className "flex flex-col text-sm text-muted-foreground"}
                ($ :span
                   (:inventory_pool_name item))
                ($ :span
                   (if (:reservation_user_name item)
                     (str (:reservation_user_name item) " until "
                          (format (:reservation_end_date item) "dd.MM.yyyy"))

                     (if isPackageItem
                       (t "pool.models.list.package_item")
                       (str (:building_name item)
                            " ( " (:building_code item) " ) "
                            " - " (t "pool.models.list.shelf") " "
                            (:shelf item))))))))

       ($ TableCell {:className "text-right"}
          ($ ItemStatus {:item item}))

       ($ TableCell {:className "fit-content"}
          ($ :div {:className "flex gap-2"}

             ($ Button {:variant "outline"
                        :asChild true}
                ($ Link {:state #js {:searchParams (.. location -search)}
                         :to (str (:id item) "/edit")
                         :viewTransition true}
                   (t "pool.models.list.actions.edit")))

             ($ DropdownMenu
                ($ DropdownMenuTrigger {:asChild "true"}
                   ($ Button {:variant "secondary"
                              :size "icon"}
                      ($ Ellipsis {:className "h-4 w-4"})))
                ($ DropdownMenuContent {:align "start"}
                   ($ DropdownMenuItem
                      ($ Link {:to (str (:id item) "/items/create")
                               :state #js {:searchParams (.. location -search)}
                               :viewTransition true}
                         (t "pool.models.list.actions.add_item"))))))))))

(def ItemRow
  (uix/as-react
   (fn [props]
     (main props))))
