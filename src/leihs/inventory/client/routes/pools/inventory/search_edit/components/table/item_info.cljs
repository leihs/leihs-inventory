(ns leihs.inventory.client.routes.pools.inventory.search-edit.components.table.item-info
  (:require
   ["date-fns" :refer [format]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router]
   [leihs.inventory.client.components.typo :refer [Typo]]
   [uix.core :as uix :refer [$ defui]]))

(defui ItemInfo [{:keys [item]}]
  (let [[t] (useTranslation)
        params (router/useParams)
        pool-id (aget params "pool-id")]

    ($ :div {:className "flex flex-col text-sm text-muted-foreground whitespace-nowrap"}
       ($ :span
          (:inventory_pool_name item))
       ($ :span
          (if (:reservation_user_name item)
            ($ Typo {:variant :link
                     :class-name "text-muted-foreground"}
               ($ :a {:href (str "/manage/" pool-id "/contracts/" (:reservation_contract_id item))
                      :target "_blank"
                      :rel "noreferrer"}
                  (str (:reservation_user_name item) " until "
                       (format (:reservation_end_date item) "dd.MM.yyyy"))))

            (str (:building_name item)
                 " ( " (:building_code item) " ) "
                 " - " (t "pool.models.list.shelf") " "
                 (:shelf item)))))))

