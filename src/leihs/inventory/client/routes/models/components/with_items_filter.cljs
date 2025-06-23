(ns leihs.inventory.client.routes.models.components.with-items-filter
  (:require
   ["@@/select" :refer [Select SelectContent SelectItem SelectTrigger
                        SelectValue]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [class-name]}]
  (let [[search-params set-search-params!] (router/useSearchParams)
        type (.. search-params (get "type"))
        [t] (useTranslation)

        with_items (js/JSON.parse (.. search-params (get "with_items")))
        handle-with-items (fn [value]
                            (if (= value nil)
                              (.delete search-params "with_items")
                              (.set search-params "with_items" value))

                            (.set search-params "page" "1")
                            (set-search-params! search-params))]

    ($ Select {:value with_items
               :disabled (= type "option")
               :onValueChange handle-with-items}
       ($ SelectTrigger {:name "with_items"
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

