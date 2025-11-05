(ns leihs.inventory.client.routes.pools.inventory.list.components.table.item-row
  (:require
   ["@@/badge" :refer [Badge]]
   ["@@/button" :refer [Button]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent
                               DropdownMenuItem DropdownMenuTrigger]]
   ["@@/table" :refer [TableCell TableRow]]
   ["lucide-react" :refer [Image ChevronDown]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [Link]]

   [leihs.inventory.client.components.image-modal :refer [ImageModal]]
   [leihs.inventory.client.routes.pools.inventory.list.components.table.item-info :refer [ItemInfo]]
   [leihs.inventory.client.routes.pools.inventory.list.components.table.item-status :refer [ItemStatus]]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [item isPackageItem]
              :or {isPackageItem false}}]
  (let [location (router/useLocation)
        [t] (useTranslation)
        ref (uix/use-ref nil)]

    ($ TableRow {:ref ref
                 :key (-> item :id)
                 :data-row "item"
                 :class-name
                 ;; checks if next sibling is a model or package row and applies drop shadow to them
                 (str
                  "[&>*]:px2 [&>*]:py-1 "
                  "[&+tr[data-row=model]]:shadow-[0_-0.5px_0_hsl(var(--border)),0_-4px_4px_-2px_hsl(var(--border))] "
                  "[&+tr[data-row=package]]:shadow-[0_-0.5px_0_hsl(var(--border)),0_-4px_4px_-2px_hsl(var(--border))] "
                  "shadow-[0_-0.5px_0_hsl(var(--border))] bg-destructive-foreground/50")}

       ($ TableCell)

       ($ TableCell {:className "text-right"}
          (if (:url item)
            ($ ImageModal {:class-name "w-7 h-7 !p-0"
                           :url (:url item)
                           :alt (str (:product item) " " (:version item))})
            ($ Image {:class-name "w-7 h-7 inline"})))

       ($ TableCell
          ($ :div {:className "flex gap-2 "}
             ($ Badge {:className "w-6 h-5 justify-center bg-blue-500"}
                (t "pool.models.list.item.badge"))))

       ($ TableCell
          ($ ItemInfo {:item item
                       :is-package-item isPackageItem}))

       ($ TableCell {:className "text-right"}
          ($ ItemStatus {:item item}))

       ($ TableCell {:className "fit-content"}
          ($ :div {:class-name
                   "flex [&>*]:rounded-none 
                   [&>a:first-child]:rounded-l-md 
                   [&>button:last-child]:rounded-r-md"}
             ($ Button {:variant "outline"
                        :asChild true}
                ($ Link {:state #js {:searchParams (.. location -search)}
                         :to (str (:id item) "/edit")
                         :viewTransition true}
                   (t "pool.models.list.actions.edit")))

             ($ DropdownMenu
                ($ DropdownMenuTrigger {:asChild true}
                   ($ Button {:data-test-id "edit-dropdown"
                              :class-name ""
                              :variant "outline"
                              :size "icon"}
                      ($ ChevronDown {:className "w-4 h-4"})))
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
