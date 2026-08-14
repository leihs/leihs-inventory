(ns leihs.inventory.client.routes.pools.inventory.list.components.table.item-status
  (:require
   ["lucide-react" :refer [SquareCheck SquareMinus
                           SquareX SquareArrowRight
                           SquareSquare SquarePause]]
   ["react-i18next" :refer [useTranslation]]
   [uix.core :as uix :refer [$ defui]]))

(defui ItemStatus [{:keys [item]}]
  (let [[t] (useTranslation)]
    ($ :div {:class-name "flex flex-col text-xs"
             :data-test-id "item-status"}

       (if (:retired item)
         ($ :span {:class-name "text-violet-500"}
            (t "pool.models.list.item.retired")
            ($ SquareSquare {:class-name "inline ml-2 h-4 w-4 "}))

         ($ :<>
            (when (:reservation_user_name item)
              ($ :span {:class-name "text-blue-500"}
                 (t "pool.models.list.item.rented")
                 ($ SquareArrowRight {:class-name "inline ml-2 h-4 w-4 "})))

            (when (:is_broken item)
              ($ :span {:class-name "text-red-500"}
                 (t "pool.models.list.item.broken")
                 ($ SquareX {:class-name "inline ml-2 h-4 w-4"})))

            (when (:is_incomplete item)
              ($ :span {:class-name "text-amber-500"}
                 (t "pool.models.list.item.incomplete")
                 ($ SquareMinus {:class-name "inline ml-2 h-4 w-4 "})))

            (when (and
                   (not (:reservation_user_name item))
                   (or (:retired item)
                       (not (:is_borrowable item))))
              ($ :span {:class-name "text-gray-500"}
                 (t "pool.models.list.item.not_borrowable")
                 ($ SquarePause {:class-name "inline ml-2 h-4 w-4 "})))

            (when (and (:is_borrowable item)
                       (not (:reservation_user_name item))
                       (not (:retired item)))
              ($ :span {:class-name "text-green-500"}
                 (t "pool.models.list.item.available")
                 ($ SquareCheck {:class-name "inline ml-2 h-4 w-4"}))))))))

