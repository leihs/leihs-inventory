(ns leihs.inventory.client.routes.pools.inventory.list.components.table.package-row
  (:require
   ["@@/badge" :refer [Badge]]
   ["@@/button" :refer [Button]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent
                               DropdownMenuItem DropdownMenuTrigger]]
   ["@@/table" :refer [Table TableBody TableCell TableHead TableHeader
                       TableRow]]
   ["date-fns" :refer [format]]
   ["lucide-react" :refer [Download Ellipsis Image ListRestart Minus Plus]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [Link]]
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.pools.inventory.list.components.table.item-row :refer [ItemRow]]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [package]}]
  (let [location (router/useLocation)
        [t] (useTranslation)
        params (router/useParams)
        pool-id (aget params "pool-id")
        [result set-result!] (uix/use-state nil)

        handle-expand (fn []
                        (if result
                          (set-result! nil)

                          (-> http-client
                              (.get (str "/inventory/" pool-id "/items/?parent_id=" (:id package))
                                    #js {:cache false})
                              (.then (fn [data]
                                       (set-result! {:status (.. data -status)
                                                     :statusText (.. data -statusText)
                                                     :data (jc (.. data -data))})))
                              (.catch (fn [err]
                                        {:status (.. err -response -status)
                                         :statusText (.. err -response -statusText)})))))]

    ($ :<>
       ($ TableRow {:key (-> package :id)
                    :class-name "bg-destructive-foreground/50"
                    :style (if result
                             {:box-shadow "0 3px 3px hsl(var(--border))"}
                             {:box-shadow "0 -0.5px 0 hsl(var(--border))"})}

          ($ TableCell
             ($ :div {:className "flex items-center gap-4 ml-2"}
                ($ Button {:variant "outline"
                           :on-click handle-expand
                           :size "icon"
                           :class-name (if
                                        (zero? (-> package :total_items))
                                         "cursor-not-allowed"
                                         "")
                           :disabled (zero? (-> package :total_items))}
                   (if result
                     ($ Minus {:className "h-4 w-4"})
                     ($ Plus {:className "h-4 w-4"})))

                ($ :span {:className "text-xl ml-auto"}
                   (-> package :package_items_count str))))

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
             ($ :div {:class-name "flex flex-row items-center"}
                ($ :span {:class-name "w-32"}
                   (:inventory_code package))
                ($ :div {:className "flex flex-col text-sm text-muted-foreground"}
                   ($ :span
                      (:inventory_pool_name package))
                   ($ :span
                      (if (:reservation_user_name package)
                        (str (:reservation_user_name package) " until "
                             (format (:reservation_end_date package) "dd.MM.yyyy"))

                        (str (:building_name package)
                             " ( " (:building_code package) " ) "
                             " - " (t "pool.models.list.shelf") " "
                             (:shelf package)))))))

          ($ TableCell {:className "text-right"}
             (cond
               (:is_borrowable package)
               "borrowable"
               (:is_broken package)
               "broken"))

          ($ TableCell {:className "fit-content"}
             ($ :div {:className "flex gap-2"}

                ($ Button {:variant "outline"
                           :asChild true}
                   ($ Link {:state #js {:searchParams (.. location -search)}
                            :to (str (:id package) "/edit")
                            :viewTransition true}
                      (t "pool.models.list.actions.edit")))

                ($ DropdownMenu
                   ($ DropdownMenuTrigger {:asChild "true"}
                      ($ Button {:variant "secondary"
                                 :size "icon"}
                         ($ Ellipsis {:className "h-4 w-4"})))
                   ($ DropdownMenuContent {:align "start"}
                      ($ DropdownMenuItem
                         ($ Link {:to (str (:id package) "/items/create")
                                  :state #js {:searchParams (.. location -search)}
                                  :viewTransition true}
                            (t "pool.models.list.actions.add_item"))))))))

       ;; render expanded rows
       (when (and result (= (:status result) 200))

         (when (seq (:data result))
           (map
            (fn [item]
              (when (not (:is_package item))
                ($ ItemRow {:key (:id item)
                            :is-package-item true
                            :item item})))
            (:data result)))))))

(def PackageRow
  (uix/as-react
   (fn [props]
     (main props))))
