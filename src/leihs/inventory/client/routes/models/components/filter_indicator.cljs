(ns leihs.inventory.client.routes.models.components.filter-indicator
  (:require
   ["@@/badge" :refer [Badge]]
   ["@@/button" :refer [Button]]
   ["lucide-react" :refer [Building CalendarDays CirclePlus List Tags Tags X]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router]
   [leihs.inventory.client.lib.utils :refer [jc]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defn find-category-name [tree-list category-id]
  (when tree-list
    (some (fn [node]
            (if (= (:category_id node) category-id)
              (:name node)
              (find-category-name (:children node) category-id)))
          tree-list)))

(defui interactive-badge [{:keys [on-click value class-name children]}]
  ($ :div {:class-name "relative group"}

     ($ Badge {:variant "secondary"
               :class-name (str "rounded-full cursor-default " class-name)}
        children)
     ($ Button {:size "icon"
                :variant "primary"
                :on-click on-click
                :value value
                :class-name "bg-secondary-foreground text-secondary absolute right-0 top-0 group-hover:right-[-3px] group-hover:top-[-3px] w-3 h-3 z-50 
                               rounded-full group-hover:opacity-100 opacity-0 transition-all"}
        ($ X {:class-name "p-[3px]"}))))

(defui main [{:keys [class-name]}]
  (let [[search-params set-search-params!] (router/useSearchParams)
        categories (:children (:categories (router/useRouteLoaderData "models-page")))
        inventory-pools (:responsible-pools (router/useRouteLoaderData "models-page"))
        [t] (useTranslation)
        entries (into [] (jc (for [entry (.. search-params (entries))] entry)))
        entries-map (into {} (map (fn [[k v]] [(keyword k) v]) entries))
        sorted-map (let [priority-keys #{:owned :incomplete :broken :in_stock}]
                     (into (sorted-map-by
                            (fn [k1 k2]
                              (cond
                                (and (priority-keys k1) (not (priority-keys k2))) 1
                                (and (priority-keys k2) (not (priority-keys k1))) -1
                                :else (compare k1 k2))))
                           entries-map))

        handle-click (fn [e]
                       (let [val (.. e -target -value)]
                         (.delete search-params val)
                         (.set search-params "page" "1")
                         (set-search-params! search-params)))]

    (for [[key value] sorted-map]
      ($ :<> {:key (str key value)}
         (cond
           (= key :owned)
           ($ interactive-badge {:value (name key)
                                 :on-click handle-click}
              ($ CirclePlus {:className "h-3 w-3 mr-2 "})
              (str (t "pool.models.filters.status.owned")
                   " - "
                   (if (= value "true") (t "pool.models.filters.status.yes")
                       (t "pool.models.filters.status.no"))))

           (= key :in_stock)
           ($ interactive-badge {:value (name key)
                                 :on-click handle-click}

              ($ CirclePlus {:className "h-3 w-3 mr-2 "})
              (str (t "pool.models.filters.status.in_stock")
                   " - "
                   (if (= value "true") (t "pool.models.filters.status.yes")
                       (t "pool.models.filters.status.no"))))

           (= key :incomplete)
           ($ interactive-badge {:value (name key)
                                 :on-click handle-click}

              ($ CirclePlus {:className "h-3 w-3 mr-2 "})
              (str (t "pool.models.filters.status.incomplete")
                   " - "
                   (if (= value "true") (t "pool.models.filters.status.yes")
                       (t "pool.models.filters.status.no"))))

           (= key :broken)
           ($ interactive-badge {:value (name key)
                                 :on-click handle-click}
              ($ CirclePlus {:className "h-3 w-3 mr-2 "})
              (str (t "pool.models.filters.status.broken")
                   " - "
                   (if (= value "true") (t "pool.models.filters.status.yes")
                       (t "pool.models.filters.status.no"))))

           (= key :before_last_check)
           ($ interactive-badge {:value (name key)
                                 :on-click handle-click}
              ($ CalendarDays {:className "h-3 w-3 mr-2 "})
              (str value))

           (= key :category_id)
           ($ interactive-badge {:value (name key)
                                 :on-click handle-click}
              ($ List {:className "h-3 w-3 mr-2 "})
              (find-category-name categories value))

           (= key :inventory_pool_id)
           ($ interactive-badge {:value (name key)
                                 :on-click handle-click}
              ($ Building {:className "h-3 w-3 mr-2"})
              (:name (first (filter #(= (:id %) value) inventory-pools))))

           (= key :type)
           ($ interactive-badge {:value (name key)
                                 :on-click handle-click}
              ($ Tags {:className "h-3 w-3 mr-2"})
              (t (str "pool.models.filters.type." value))))))))

(def FilterIndicator
  (uix/as-react
   (fn [props]
     (main props))))
