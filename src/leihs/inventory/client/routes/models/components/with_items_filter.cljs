(ns leihs.inventory.client.routes.models.components.with-items-filter
  (:require
   ["@@/select" :refer [Select SelectContent SelectItem SelectTrigger
                        SelectValue]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router]
   [leihs.inventory.client.routes.models.filter-reducer :as filter-reducer]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [class-name]}]
  (let [[search-params _] (router/useSearchParams)
        [t] (useTranslation)

        dispatch (filter-reducer/use-filter-dispatcher)
        state (filter-reducer/use-filter-state)

        with_items (js/JSON.parse (.. search-params (get "with_items")))
        handle-with-items (fn [value]
                            (if (= value nil)
                              (dispatch {:filter "with_items" :value nil :delete true})
                              (dispatch {:filter "with_items" :value value})))]

    ($ Select {:value with_items
               :onValueChange handle-with-items}
       ($ SelectTrigger {:name "with_items"
                         :disabled (some #{:with_items} state)
                         :className (str "w-[260px] " class-name)}
          ($ SelectValue))
       ($ SelectContent
          ($ SelectItem {:data-test-id "all"
                         :value nil}
             (t "pool.models.filters.with_items.all"))
          ($ SelectItem {:data-test-id "with_items"
                         :value true}
             (t "pool.models.filters.with_items.with_items"))
          ($ SelectItem {:data-test-id "without_items"
                         :value false}
             (t "pool.models.filters.with_items.without_items"))))))

(def WithItemsFilter
  (uix/as-react
   (fn [props]
     (main props))))

