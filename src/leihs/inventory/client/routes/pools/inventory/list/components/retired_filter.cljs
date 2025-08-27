(ns leihs.inventory.client.routes.pools.inventory.list.components.retired-filter
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
        retired (js/JSON.parse (.. search-params (get "retired")))
        handle-retired (fn [value]
                         (if (= value nil)
                           (.delete search-params "retired")
                           (.set search-params "retired" value))
                         (.set search-params "page" "1")
                         (set-search-params! search-params))]

    ($ Select {:value retired
               :disabled (= type "option")
               :onValueChange handle-retired}
       ($ SelectTrigger {:name "retired"
                         :className (str "w-[280px]" class-name)}
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

