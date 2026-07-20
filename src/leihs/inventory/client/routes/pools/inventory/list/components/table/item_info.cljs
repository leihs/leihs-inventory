(ns leihs.inventory.client.routes.pools.inventory.list.components.table.item-info
  (:require
   ["react-i18next" :refer [useTranslation]]
   ["react-router" :as router]
   [leihs.inventory.client.components.typo :refer [Typo]]
   [leihs.inventory.client.lib.date-helper :refer [string-to-plain-date]]
   [uix.core :as uix :refer [$ defui]]))

(defui ItemInfo [{:keys [item is-package-item is-software-license]
                  :or {is-package-item false}}]
  (let [[t] (useTranslation)
        {:keys [fields]} (router/useLoaderData)
        params (router/useParams)
        pool-id (aget params "pool-id")
        field-value-labels (into {}
                                 (for [field fields
                                       :when (:values field)]
                                   [(:id field)
                                    (into {} (map (fn [v] [(:value v) (:label v)]) (:values field)))]))
        get-label (fn [field-id value]
                    (get-in field-value-labels [(name field-id) value] value))]

    (if is-package-item
      ($ :div {:class-name "flex flex-row items-center"
               :data-test-id "item-info"}
         ($ :span {:class-name "w-32"}
            (:inventory_code item))

         ($ :div {:class-name "flex flex-col text-sm text-muted-foreground"}
            ($ :span
               (:model_name item))
            ($ :span
               (t "pool.models.list.package_item"))))

      ($ :div {:class-name "flex flex-row items-center"}
         ($ :span {:class-name "w-32"}
            (:inventory_code item))

         ($ :div {:class-name "flex flex-col text-sm text-muted-foreground"}
            (when (not= (:inventory_pool_id item)
                        (:owner_id item))
              ($ :span
                 (:inventory_pool_name item)))

            ($ :div {:class-name "flex flex-wrap gap-1"}
               (if (:reservation_user_name item)
                 ($ Typo {:variant :link
                          :class-name "text-muted-foreground"}
                    ($ :a {:href (str "/manage/" pool-id "/contracts/" (:reservation_contract_id item))
                           :target "_blank"
                           :rel "noreferrer"}
                       (str (:reservation_user_name item) " " (t "pool.models.list.info.until") " "
                            (t "intlDateTime" #js {:val (string-to-plain-date (:reservation_end_date item))}))))

                 (if is-software-license
                   (let [os-values (seq (:properties_operating_system item))
                         entries (filterv some?
                                          (concat
                                           (when (not-empty (:item_version item))
                                             [{:key "item_version" :label (:item_version item)}])
                                           (when os-values
                                             (map (fn [v] {:key (str "os-" v) :label (get-label :properties_operating_system v)}) os-values))
                                           (when (not-empty (:properties_license_type item))
                                             [{:key "license_type" :label (get-label :properties_license_type (:properties_license_type item))}])
                                           (when (some? (:properties_total_quantity item))
                                             [{:key "total_quantity" :label (:properties_total_quantity item)}])))
                         last-idx (dec (count entries))]
                     ($ :<>
                        (map-indexed
                         (fn [idx {:keys [key label]}]
                           ($ :span {:key key}
                              (if (< idx last-idx)
                                (str label ",")
                                label)))
                         entries)))

                   ($ :<>
                      ($ :span
                         (str (:building_name item)
                              "( " (:building_code item) " )"))

                      (when (or (:room_name item)
                                (:shelf item))
                        " - ")

                      (when (:room_name item)
                        ($ :span (:room_name item)))

                      (when (:room_name item)
                        ($ :span (:shelf item))))))))))))

