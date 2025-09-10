(ns leihs.inventory.client.routes.pools.inventory.list.components.table.item-status
  (:require
   ["lucide-react" :refer [SquareCheck SquareMinus
                           SquareX SquareArrowRight
                           SquarePause]]
   ["react-i18next" :refer [useTranslation]]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [item]}]
  (let [[t] (useTranslation)]
    ($ :div {:class-name "flex flex-col"}
       (when (:reservation_user_name item)
         ($ :span {:className "text-blue-500"}
            (t "pool.models.list.item.rented")
            ($ SquareArrowRight {:className "inline ml-2 h-4 w-4 "})))

       (when (:is_broken item)
         ($ :span {:className "text-rose-500"}
            (t "pool.models.list.item.broken")
            ($ SquareX {:className "inline ml-2 h-4 w-4"})))

       (when (:is_incomplete item)
         ($ :span {:className "text-amber-500"}
            (t "pool.models.list.item.incomplete")
            ($ SquareMinus {:className "inline ml-2 h-4 w-4 "})))

       (when (not (:is_borrowable item))
         ($ :span {:className "text-gray-500"}
            (t "pool.models.list.item.not_borrowable")
            ($ SquarePause {:className "inline ml-2 h-4 w-4 "})))

       (when (and (:is_borrowable item)
                  (not (:reservation_user_name item)))
         ($ :span {:className "text-green-500"}
            (t "pool.models.list.item.available")
            ($ SquareCheck {:className "inline ml-2 h-4 w-4"}))))))

(def ItemStatus
  (uix/as-react
   (fn [props]
     (main props))))
