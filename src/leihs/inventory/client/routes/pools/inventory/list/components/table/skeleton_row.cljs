(ns leihs.inventory.client.routes.pools.inventory.list.components.table.skeleton-row
  (:require
   ["@@/table" :refer [TableCell
                       TableRow]]
   ["lucide-react" :refer [Loader2Icon]]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [col-span]}]
  ($ TableRow {:class-name "animate-pulse"}
     ($ TableCell {:col-span col-span
                   :class-name "h-12 text-center"}
        ($ :div {:class-name "flex justify-center items-center gap-2"}
           ($ Loader2Icon {:class-name "h-6 w-6 animate-spin"})
           "Loading..."))))

(def SkeletonRow
  (uix/as-react
   (fn [props]
     (main props))))
