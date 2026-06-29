(ns leihs.inventory.client.components.item-info
  (:require
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router]
   [leihs.inventory.client.components.typo :refer [Typo]]
   [leihs.inventory.client.lib.date-helper :refer [string-to-plain-date]]
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
                  (str (:reservation_user_name item) " " (t "pool.models.search_edit.info.until") " "
                       (t "intlDateTime" #js {:val (string-to-plain-date (:reservation_end_date item))}))))

            (str (:building_name item)
                 " ( " (:building_code item) " ) "
                 " - " (t "pool.models.list.shelf") " "
                 (:shelf item)))))))

