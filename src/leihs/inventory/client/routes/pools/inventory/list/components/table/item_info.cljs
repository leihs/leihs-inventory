(ns leihs.inventory.client.routes.pools.inventory.list.components.table.item-info
  (:require
   ["date-fns" :refer [format]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [item isPackageItem]
              :or {isPackageItem false}}]
  (let [[t] (useTranslation)
        params (router/useParams)
        pool-id (aget params "pool-id")
        has-location-data? (or (:inventory_pool_name item)
                               (:reservation_user_name item)
                               (:building_name item)
                               (:building_code item)
                               (:shelf item))]

    ($ :div {:class-name "flex flex-row items-center"
             :data-test-id "item-info"}
       ($ :span {:class-name "w-32"}
          (:inventory_code item))
       ($ :div {:className "flex flex-col text-sm text-muted-foreground"}
          (if has-location-data?
            ($ :<>
               ($ :span
                  (:inventory_pool_name item))
               ($ :span
                  (if (:reservation_user_name item)
                    ($ :a {:href (str "/manage/" pool-id "/contracts/" (:reservation_contract_id item))
                           :target "_blank"
                           :rel "noreferrer"}
                       (str (:reservation_user_name item) " until "
                            (format (:reservation_end_date item) "dd.MM.yyyy")))

                    (str (:building_name item)
                         " ( " (:building_code item) " ) "
                         " - " (t "pool.models.list.shelf") " "
                         (:shelf item)))))

            ;; Keep old package-item fallback when location fields are not provided.
            ($ :span
               (:model_name item)))
          (when (and isPackageItem (or has-location-data? (:model_name item)))
            ($ :span
               (t "pool.models.list.package_item")))))))

(def ItemInfo
  (uix/as-react
   (fn [props]
     (main props))))
