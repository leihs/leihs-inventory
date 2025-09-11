(ns leihs.inventory.client.routes.pools.inventory.list.components.table.item-info
  (:require
   ["date-fns" :refer [format]]
   ["react-i18next" :refer [useTranslation]]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [item isPackageItem]
              :or {isPackageItem false}}]
  (let [[t] (useTranslation)]
    (if isPackageItem
      ($ :div {:class-name "flex flex-row items-center"}
         ($ :span {:class-name "w-32"}
            (:inventory_code item))
         ($ :div {:className "flex flex-col text-sm text-muted-foreground"}
            ($ :span
               (:model_name item))
            ($ :span
               (t "pool.models.list.package_item"))))

      ($ :div {:class-name "flex flex-row items-center"}
         ($ :span {:class-name "w-32"}
            (:inventory_code item))
         ($ :div {:className "flex flex-col text-sm text-muted-foreground"}
            ($ :span
               (:inventory_pool_name item))
            ($ :span
               (if (:reservation_user_name item)
                 (str (:reservation_user_name item) " until "
                      (format (:reservation_end_date item) "dd.MM.yyyy"))

                 (str (:building_name item)
                      " ( " (:building_code item) " ) "
                      " - " (t "pool.models.list.shelf") " "
                      (:shelf item)))))))))

(def ItemInfo
  (uix/as-react
   (fn [props]
     (main props))))
