(ns leihs.inventory.client.routes.pools.inventory.list.components.table.skeleton-row
  (:require
   ["@@/button" :refer [Button]]
   ["@@/button-group" :refer [ButtonGroup]]
   ["@@/skeleton" :refer [Skeleton]]
   ["@@/table" :refer [TableCell TableRow]]
   ["lucide-react" :refer [Ellipsis Image ChevronDown]]
   ["react-i18next" :refer [useTranslation]]

   [uix.core :as uix :refer [$ defui]]))

(defui SkeletonRow [{:keys [permission]}]
  (let [[t] (useTranslation)]

    ($ TableRow {:class-name "shadow-[0_-0.5px_0_hsl(var(--border))] "}
       ($ TableCell
          ($ Skeleton {:class-name "w-[76px] h-9 ml-2"}))

       ($ TableCell
          ($ Skeleton {:class-name "w-12 h-12"}))

       ($ TableCell
          ($ :div {:class-name "flex gap-2"}
             ($ Skeleton {:class-name "w-6 h-6"})))

       ($ TableCell
          ($ Skeleton {:class-name "w-auto h-6"}))

       ($ TableCell
          ($ Skeleton {:class-name "w-auto h-6"}))

       ($ TableCell
          ($ Skeleton
             (if (= permission "read")
               ($ Button {:variant "outline"
                          :class-name "invisible"}
                  (t "pool.models.list.actions.timeline"))

               ($ ButtonGroup {:class-name "invisible"}
                  ($ Button {:variant "outline"
                             :class-name ""}
                     (t "pool.models.list.actions.edit"))

                  ($ Button {:data-test-id "edit-dropdown"
                             :class-name ""
                             :variant "outline"
                             :size "icon"}
                     ($ ChevronDown {:class-name "w-4 h-4"})))))))))

