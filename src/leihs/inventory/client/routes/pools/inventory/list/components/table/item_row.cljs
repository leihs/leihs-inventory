(ns leihs.inventory.client.routes.pools.inventory.list.components.table.item-row
  (:require
   ["@@/badge" :refer [Badge]]
   ["@@/button" :refer [Button]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent
                               DropdownMenuItem DropdownMenuTrigger]]
   ["@@/table" :refer [TableCell TableRow]]
   ["lucide-react" :refer [Ellipsis Image]]
   ["react-i18next" :refer [useTranslation]]

   ["react-router-dom" :as router :refer [Link]]
   [leihs.inventory.client.routes.pools.inventory.list.components.table.item-info :refer [ItemInfo]]
   [leihs.inventory.client.routes.pools.inventory.list.components.table.item-status :refer [ItemStatus]]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [item]}]
  (let [location (router/useLocation)
        [t] (useTranslation)
        ref (uix/use-ref nil)
        [is-last set-is-last!] (uix/use-state false)]

    (uix/use-effect
     (fn []
       (when (.. ref -current)
         (set-is-last! (= (.. ref
                              -current
                              -nextElementSibling
                              -dataset
                              -row)
                          "expandable"))))
     [item])

    ($ TableRow {:ref ref
                 :key (-> item :id)
                 :data-row "item"
                 :style (if is-last
                          {:box-shadow
                           "0 -0.5px 0 hsl(var(--border)),
                            inset 0 -3px 4px -2px hsl(var(--border))"}
                          {:box-shadow "0 -0.5px 0 hsl(var(--border))"})
                 :class-name "bg-destructive-foreground/50 hover:bg-destructive-foreground/50"}

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
          ($ ItemInfo {:item item}))

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
