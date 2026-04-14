(ns leihs.inventory.client.routes.pools.inventory.search-edit.components.table.items-table
  (:require
   ["@@/badge" :refer [Badge]]
   ["@@/button" :refer [Button]]
   ["@@/button-group" :refer [ButtonGroup]]
   ["@@/checkbox" :refer [Checkbox]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent
                               DropdownMenuItem DropdownMenuTrigger]]
   ["@@/skeleton" :refer [Skeleton]]
   ["@@/table" :refer [Table TableBody TableCell TableHead
                       TableHeader TableRow]]
   ["lucide-react" :refer [Image ChevronDown]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [Link]]
   [leihs.inventory.client.components.image-modal :refer [ImageModal]]
   [leihs.inventory.client.components.typo :refer [Typo]]
   [leihs.inventory.client.routes.pools.inventory.list.components.table.item-status :refer [ItemStatus]]
   [leihs.inventory.client.routes.pools.inventory.search-edit.components.table.item-info :refer [ItemInfo]]
   [uix.core :as uix :refer [$ defui]]))

(defui SkeletonItemRow []
  ($ TableRow {:class-name "bg-secondary/25"}

     ($ TableCell
        ($ Checkbox {:disabled true}))

     ($ TableCell {:className "text-right"}
        ($ Skeleton {:className "w-7 h-7"}))

     ($ TableCell
        ($ :div {:className "flex gap-2"}
           ($ Skeleton {:className "w-6 h-6"})))

     ($ TableCell
        ($ Skeleton {:className "w-auto h-6"}))

     ($ TableCell {:className "text-right"}
        ($ Skeleton {:className "w-auto h-6"}))

     ($ TableCell
        ($ Skeleton {:className ""}
           ($ ButtonGroup {:class-name "invisible"}
              ($ Button {:variant "outline"}
                 "Edit")
              ($ Button {:variant "outline" :size "icon"}
                 ($ ChevronDown {:className "w-4 h-4"})))))))

(defui ItemsTable [{:keys [items loading? selected on-selection-change]}]
  (let [[t] (useTranslation)
        location (router/useLocation)

        all-selected? (and (seq items) (= (count selected) (count items)))
        some-selected? (and (seq selected) (not all-selected?))

        toggle-all (fn [checked]
                     (let [new-selection (if checked (set (map :id items)) #{})]
                       (when on-selection-change
                         (on-selection-change new-selection))))

        toggle-item (fn [id]
                      (let [new-selection (if (contains? selected id)
                                            (disj selected id)
                                            (conj selected id))]
                        (when on-selection-change
                          (on-selection-change new-selection))))]

    ($ :div {:class-name "border rounded-md"}
       ($ :div {:class-name "overflow-x-auto"}
          ($ Table
             ($ TableHeader
                ($ TableRow
                   ($ TableHead {:class-name "w-12"}
                      ($ Checkbox {:checked (cond
                                              some-selected? "indeterminate"
                                              all-selected? true
                                              :else false)
                                   :onCheckedChange toggle-all}))
                   ($ TableHead {:class-name "text-right"} "")
                   ($ TableHead (t "pool.models.search_edit.table.inventory_code"))
                   ($ TableHead
                      (t "pool.models.list.header.name"))
                   ($ TableHead (t "pool.models.search_edit.table.location"))

                   ($ TableHead {:className "min-w-40 text-right"}
                      (t "pool.models.list.header.availability"))
                   ($ TableHead {:class-name "rounded-tr-md sticky"} "")))

             ($ TableBody
                (cond
                  loading?
                  (doall (for [i (range 5)]
                           ($ SkeletonItemRow {:key i})))

                  (empty? items)
                  ($ TableRow
                     ($ TableCell {:colSpan 6
                                   :class-name "h-24 text-center"}
                        (t "pool.models.search_edit.no_items")))

                  :else
                  (doall (for [item items]
                           ($ TableRow {:key (:id item)}

                              ($ TableCell
                                 ($ Checkbox {:checked (contains? selected (:id item))
                                              :onCheckedChange #(toggle-item (:id item))}))

                              ($ TableCell {:className "text-right"}
                                 (if (:url item)
                                   ($ ImageModal {:class-name "min-w-12 w-12 h-12"
                                                  :url (:url item)
                                                  :alt (str (:model_name item))})
                                   ($ Image {:class-name "w-7 h-7 inline"})))

                              ($ TableCell
                                 (:inventory_code item))

                              ($ TableCell
                                 ($ :div {:class-name "w-90 overflow-hidden whitespace-nowrap text-ellipsis"}
                                    ($ Badge {:className "w-6 h-5 justify-center bg-blue-500 mr-2"}
                                       (t "pool.models.list.item.badge"))
                                    ($ Typo {:variant "label"}
                                       (:model_name item))))

                              ($ TableCell
                                 ($ ItemInfo {:item item}))

                              ($ TableCell {:className "text-right"}
                                 ($ ItemStatus {:item item}))

                              ($ TableCell
                                 ($ Button {:variant "outline"
                                            :asChild true}
                                    ($ Link {:state #js {:searchParams (.. location -search)}
                                             :to (str "../items/" (:id item))
                                             :viewTransition true}
                                       (t "pool.models.list.actions.edit"))))))))))))))
