(ns leihs.inventory.client.routes.models.components.retired-filter
  (:require
   ["@@/select" :refer [Select SelectContent SelectItem SelectTrigger
                        SelectValue]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router]
   [leihs.inventory.client.routes.models.filter-reducer :as filter-reducer]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [class-name]}]
  (let [[search-params set-search-params!] (router/useSearchParams)
        [t] (useTranslation)

        dispatch (filter-reducer/use-filter-dispatcher)
        state (filter-reducer/use-filter-state)

        retired (js/JSON.parse (.. search-params (get "retired")))
        handle-retired (fn [value]
                         (if (= value nil)
                           (.delete search-params "retired")
                           (.set search-params "retired" value))
                         (.set search-params "page" "1")
                         (set-search-params! search-params))]

    ($ Select {:value retired
               :onValueChange handle-retired}
       ($ SelectTrigger {:name "retired"
                         :className (str "w-[280px] "
                                         (when (-> state :hidden :retired) "hidden ")
                                         class-name)}
          ($ SelectValue))
       ($ SelectContent
          ($ SelectItem {:data-test-id "all"
                         :value nil}
             (t "pool.models.filters.retired.all"))
          ($ SelectItem {:data-test-id "retired"
                         :value true}
             (t "pool.models.filters.retired.retired"))
          ($ SelectItem {:data-test-id "not_retired"
                         :value false}
             (t "pool.models.filters.retired.not_retired"))))))

(def RetiredFilter
  (uix/as-react
   (fn [props]
     (main props))))

