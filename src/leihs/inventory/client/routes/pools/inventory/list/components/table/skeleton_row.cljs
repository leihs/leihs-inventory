(ns leihs.inventory.client.routes.pools.inventory.list.components.table.skeleton-row
  (:require
   ["@@/skeleton" :refer [Skeleton]]
   ["@@/table" :refer [TableCell TableRow]]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [col-span]}]
  ($ TableRow {:class-name "shadow-[0_-0.5px_0_hsl(var(--border))] "}

     ($ TableCell
        ($ :div
           ($ Skeleton {:className "w-9 h-9 ml-2 mr-4"})))

     ($ TableCell
        ($ Skeleton {:className "w-12 h-12"}))

     ($ TableCell
        ($ :div {:className "flex gap-2"}
           ($ Skeleton {:className "w-6 h-6"})))

     ($ TableCell
        ($ Skeleton {:className "w-auto h-6"}))

     ($ TableCell
        ($ Skeleton {:className "w-auto h-6"}))

     ($ TableCell
        ($ Skeleton {:className "w-[94.38px] h-6"}))))

(def SkeletonRow
  (uix/as-react
   (fn [props]
     (main props))))
