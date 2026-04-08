(ns leihs.inventory.client.routes.pools.inventory.list.components.table.item-info
  (:require
   ["date-fns" :refer [format]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router]
   [clojure.string :as str]
   [uix.core :as uix :refer [$ defui]]))

(defn- operating-system-labels-str [t item]
  (let [raw (:properties_operating_system item)
        slugs (cond (vector? raw) raw
                    (some? raw) [raw]
                    :else nil)]
    (when (seq slugs)
      (str/join ", "
                (map (fn [s]
                       (let [slug (if (keyword? s) (name s) (str s))]
                         (t (str "pool.models.list.operating_system." slug)
                            #js {:defaultValue slug})))
                     slugs)))))

(defn- license-type-line-str [t item]
  (when-let [raw (:properties_license_type item)]
    (let [slug (str/trim (if (keyword? raw) (name raw) (str raw)))]
      (when-not (str/blank? slug)
        (t (str "pool.models.list.license_type." slug)
           #js {:defaultValue slug})))))

(defn- owner-differs-from-responsible-pool? [item]
  (let [owner (:owner_id item)
        responsible (:inventory_pool_id item)]
    (and (some? owner) (some? responsible)
         (not= (str owner) (str responsible)))))

(defn- model-item-location-line-str [t item]
  (let [shelf-raw (:shelf item)
        shelf (when (some? shelf-raw)
                (str/trim (if (keyword? shelf-raw) (name shelf-raw) (str shelf-raw))))
        has-shelf? (not (str/blank? shelf))
        room-nm (when-let [rn (:room_name item)]
                  (str/trim (str rn)))
        room-ok? (not (str/blank? room-nm))]
    (if has-shelf?
      (str (:building_name item)
           " ( " (:building_code item) " ) "
           " - " (t "pool.models.list.shelf") " "
           shelf)
      (if room-ok?
        (str (:building_name item)
             " ( " (:building_code item) " ) "
             " - " (t "pool.models.list.room") " "
             room-nm)
        ;; No shelf and no room name: show building only (avoid bare "Room ")
        (str (:building_name item)
             " ( " (:building_code item) " ) ")))))

(defui main [{:keys [item isPackageItem type]
              :or {isPackageItem false}}]
  (let [[t] (useTranslation)
        params (router/useParams)
        pool-id (aget params "pool-id")]
    (if isPackageItem
      ($ :div {:class-name "flex flex-row items-center"
               :data-test-id "item-info"}
         ($ :span {:class-name "w-32"}
            (:inventory_code item))
         ($ :div {:className "flex flex-col text-sm text-muted-foreground"}
            ($ :span (:model_name item))
            ($ :span (t "pool.models.list.package_item"))))
      (let [license-type-line (when (= type "Software")
                                (license-type-line-str t item))
            show-responsible-pool? (or (not= type "Software")
                                       (owner-differs-from-responsible-pool? item))]
        ($ :div {:class-name "flex flex-row items-center"}
           ($ :span {:class-name "w-32"}
              (:inventory_code item))
           ($ :div {:className "flex flex-col text-sm text-muted-foreground gap-0.5"}
              (when show-responsible-pool?
                ($ :span (:inventory_pool_name item)))
              (if (:reservation_user_name item)
                ($ :<>
                   ($ :span
                      ($ :a {:href (str "/manage/" pool-id "/contracts/" (:reservation_contract_id item))
                             :target "_blank"
                             :rel "noreferrer"}
                         (str (:reservation_user_name item) " until "
                              (format (:reservation_end_date item) "dd.MM.yyyy"))))
                   (when license-type-line
                     ($ :span license-type-line)))
                ($ :<>
                   (if (= type "Software")
                     (when-let [os (operating-system-labels-str t item)]
                       (when-not (str/blank? os)
                         ($ :span os)))
                     ($ :span (model-item-location-line-str t item)))
                   (when license-type-line
                     ($ :span license-type-line))))))))))

(def ItemInfo
  (uix/as-react
   (fn [props]
     (main props))))
