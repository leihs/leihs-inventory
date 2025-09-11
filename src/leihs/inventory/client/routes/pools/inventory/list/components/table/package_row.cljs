(ns leihs.inventory.client.routes.pools.inventory.list.components.table.package-row
  (:require
   ["@@/badge" :refer [Badge]]
   ["@@/button" :refer [Button]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent
                               DropdownMenuItem DropdownMenuTrigger]]
   ["@@/table" :refer [TableCell]]
   ["lucide-react" :refer [Image ChevronDown]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [Link]]
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.pools.inventory.list.components.table.expandable-row :refer [ExpandableRow]]
   [leihs.inventory.client.routes.pools.inventory.list.components.table.item-info :refer [ItemInfo]]
   [leihs.inventory.client.routes.pools.inventory.list.components.table.item-row :refer [ItemRow]]
   [leihs.inventory.client.routes.pools.inventory.list.components.table.item-status :refer [ItemStatus]]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [package]}]
  (let [location (router/useLocation)
        [t] (useTranslation)
        params (router/useParams)
        pool-id (aget params "pool-id")
        [search-params _] (router/useSearchParams)
        [result set-result!] (uix/use-state nil)

        handle-expand (fn []
                        (if result
                          (set-result! nil)

                          (-> http-client
                              (.get (str "/inventory/" pool-id "/items/")
                                    (cj {:params {:parent_id (:id package)}
                                         :cache false}))
                              (.then (fn [data]
                                       (set-result! {:status (.. data -status)
                                                     :statusText (.. data -statusText)
                                                     :data (jc (.. data -data))})))
                              (.catch (fn [err]
                                        {:status (.. err -response -status)
                                         :statusText (.. err -response -statusText)})))))]
    (uix/use-effect
     (fn []
       (set-result! nil))
     [search-params])

    ($ ExpandableRow {:key (-> package :id)
                      :data-row "package"
                      :subrow-count (:package_items_count package)
                      :class-name (str
                                    ;; checks if next sibling is a model or item 
                                    ;; item -> inset shadow 
                                    ;; model -> dropshadow
                                   "[&+tr[data-row='item']]:shadow-[0_-0.5px_0_hsl(var(--border)),inset_0_4px_4px_-2px_hsl(var(--border))] "
                                   "[&+tr[data-row='model']]:shadow-[0_-0.5px_0_hsl(var(--border)),0_-4px_4px_-2px_hsl(var(--border))] "
                                   "shadow-[0_-0.5px_0_hsl(var(--border))] bg-destructive-foreground/50")
                      :on-expand handle-expand
                      :subrows (when (and result (= (:status result) 200))
                                 (when (seq (:data result))
                                   (map
                                    (fn [item]
                                      (when (not (:is_package item))
                                        ($ ItemRow {:key (:id item)
                                                    :is-package-item true
                                                    :item item})))
                                    (:data result))))}

       ($ TableCell
          (if (:url package)
            ($ :img {:class-name "w-12 h-12 object-contain"
                     :src (:url package)
                     :alt (str (:product package) " " (:version package))})
            ($ Image {:class-name "w-12 h-12"})))

       ($ TableCell
          ($ :div {:className "flex gap-2"}
             ($ Badge {:className "w-6 h-6 justify-center bg-lime-500"} "P")))

       ($ TableCell {:className ""}
          ($ ItemInfo {:item package}))

       ($ TableCell {:className "text-right"}
          ($ ItemStatus {:item package}))

       ($ TableCell {:className "fit-content"}
          ($ :div {:class-name
                   "flex [&>*]:rounded-none 
                   [&>a:first-child]:rounded-l-md 
                   [&>button:last-child]:rounded-r-md"}
             ($ Button {:variant "outline"
                        :asChild true}
                ($ Link {:state #js {:searchParams (.. location -search)}
                         :to (str (:id package) "/edit")
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
                      ($ Link {:to (str (:id package) "/items/create")
                               :state #js {:searchParams (.. location -search)}
                               :viewTransition true}
                         (t "pool.models.list.actions.add_item"))))))))))

(def PackageRow
  (uix/as-react
   (fn [props]
     (main props))))
