(ns leihs.inventory.client.routes.pools.inventory.list.components.table.skeleton-row
  (:require
   ["@@/button" :refer [Button]]
   ["@@/button-group" :refer [ButtonGroup]]
   ["@@/skeleton" :refer [Skeleton]]
   ["@@/table" :refer [TableCell TableRow]]
   ["lucide-react" :refer [Ellipsis Image ChevronDown]]
   ["react-i18next" :refer [useTranslation]]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [col-span]}]
  (let [[t] (useTranslation)]

    ($ TableRow {:class-name "shadow-[0_-0.5px_0_hsl(var(--border))] "}

       ($ TableCell
          ($ Skeleton {:className "w-[76px] h-9 ml-2"}))

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
          ($ Skeleton {:className ""}
             ($ ButtonGroup {:class-name "invisible"}
                ($ Button {:variant "outline"
                           :class-name ""}
                   (t "pool.models.list.actions.edit"))

                ($ Button {:data-test-id "edit-dropdown"
                           :class-name ""
                           :variant "outline"
                           :size "icon"}
                   ($ ChevronDown {:className "w-4 h-4"}))))))))

(def SkeletonRow
  (uix/as-react
   (fn [props]
     (main props))))
