(ns leihs.inventory.client.components.items-table
  (:require
   ["@@/badge" :refer [Badge]]
   ["@@/button" :refer [Button]]
   ["@@/button-group" :refer [ButtonGroup]]
   ["@@/checkbox" :refer [Checkbox]]
   ["@@/skeleton" :refer [Skeleton]]
   ["@@/table" :refer [Table TableBody TableCell TableHead TableHeader TableRow]]
   ["lucide-react" :refer [ChevronDown Image]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router" :refer [Link]]
   [leihs.inventory.client.components.image-modal :refer [ImageModal]]
   [leihs.inventory.client.components.item-info :refer [ItemInfo]]
   [leihs.inventory.client.components.typo :refer [Typo]]
   [leihs.inventory.client.routes.pools.inventory.list.components.table.item-status :refer [ItemStatus]]
   [uix.core :as uix :refer [$ defui]]))

(defui SkeletonItemRow [{:keys [selectable? row-action]}]
  ($ TableRow {:class-name "bg-secondary/25"}
     (when selectable?
       ($ TableCell
          ($ Checkbox {:disabled true})))

     ($ TableCell {:class-name "text-right"}
        ($ Skeleton {:class-name "w-7 h-7"}))

     ($ TableCell
        ($ :div {:class-name "flex gap-2"}
           ($ Skeleton {:class-name "w-6 h-6"})))

     ($ TableCell
        ($ Skeleton {:class-name "w-auto h-6"}))

     ($ TableCell {:class-name "text-right"}
        ($ Skeleton {:class-name "w-auto h-6"}))

     (when row-action
       ($ TableCell
          ($ ButtonGroup {:class-name "invisible"}
             ($ Button {:variant "outline"}
                "Edit")
             ($ Button {:variant "outline" :size "icon"}
                ($ ChevronDown {:className "w-4 h-4"})))))))

(defui ItemsTable [{:keys [items loading? selected on-selection-change row-action]}]
  (let [[t] (useTranslation)
        selectable? (some? on-selection-change)
        col-count (cond-> 4
                    selectable? inc
                    row-action inc)

        all-selected? (and (seq items) (= (count selected) (count items)))
        some-selected? (and (seq selected) (not all-selected?))

        toggle-all (fn [checked]
                     (on-selection-change (if checked (set (map :id items)) #{})))

        toggle-item (fn [id]
                      (on-selection-change (if (contains? selected id)
                                             (disj selected id)
                                             (conj selected id))))]

    ($ :div {:class-name "border rounded-md"}
       ($ :div {:class-name "overflow-x-auto"}
          ($ Table
             ($ TableHeader
                ($ TableRow
                   (when selectable?
                     ($ TableHead {:class-name "w-12"}
                        ($ Checkbox {:checked (cond
                                                some-selected? "indeterminate"
                                                all-selected? true
                                                :else false)
                                     :onCheckedChange toggle-all})))
                   ($ TableHead {:class-name "text-right"} "")
                   ($ TableHead (t "pool.models.search_edit.table.inventory_code"))
                   ($ TableHead (t "pool.models.list.header.name"))
                   ($ TableHead (t "pool.models.search_edit.table.location"))
                   ($ TableHead {:className "min-w-40 text-right sticky"}
                      (t "pool.models.list.header.availability"))
                   (when row-action
                     ($ TableHead ""))))

             ($ TableBody
                (cond
                  loading?
                  (doall (for [i (range 5)]
                           ($ SkeletonItemRow {:key i
                                               :selectable? selectable?
                                               :row-action row-action})))

                  (empty? items)
                  ($ TableRow
                     ($ TableCell {:colSpan col-count
                                   :class-name "h-24 text-center"}
                        (t "pool.models.search_edit.no_items")))

                  :else
                  (doall (for [item items]
                           ($ TableRow {:key (:id item)}
                              (when selectable?
                                ($ TableCell
                                   ($ Checkbox {:checked (contains? selected (:id item))
                                                :onCheckedChange #(toggle-item (:id item))})))

                              ($ TableCell {:class-name "text-right"}
                                 (if (:url item)
                                   ($ ImageModal {:class-name "min-w-12 w-12 h-12"
                                                  :url (:url item)
                                                  :alt (str (:model_name item))})
                                   ($ Image {:class-name "w-7 h-7 inline"})))

                              ($ TableCell
                                 ($ Typo {:variant "link"}
                                    ($ Link {:to (str "../items/" (:id item))
                                             :viewTransition true}
                                       (:inventory_code item))))

                              ($ TableCell
                                 ($ :div {:class-name "w-90 overflow-hidden whitespace-nowrap text-ellipsis"}
                                    ($ Badge {:class-name "w-6 h-5 justify-center bg-blue-500 mr-2"}
                                       (t "pool.models.list.item.badge"))
                                    ($ Typo {:variant "label"}
                                       (:model_name item))))

                              ($ TableCell
                                 ($ ItemInfo {:item item}))

                              ($ TableCell {:className "text-right"}
                                 ($ ItemStatus {:item item}))

                              (when row-action
                                ($ TableCell {:className "text-right"}
                                   (row-action item)))))))))))))
