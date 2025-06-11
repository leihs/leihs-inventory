(ns leihs.inventory.client.routes.models.components.borrowable-filter
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

        borrowable (js/JSON.parse (.. search-params (get "borrowable")))
        handle-borrowable (fn [value]
                            (if (= value nil)
                              (.delete search-params "borrowable")
                              (.set search-params "borrowable" value))
                            (.set search-params "page" "1")
                            (set-search-params! search-params))]

    ($ Select {:value borrowable
               :onValueChange handle-borrowable}
       ($ SelectTrigger {:name "borrowable"
                         :disabled (-> state :hidden :borrowable)
                         :className (str "w-[260px] " class-name)}
          ($ SelectValue))
       ($ SelectContent
          ($ SelectItem {:data-test-id "all"
                         :value nil}
             (t "pool.models.filters.borrowable.all"))
          ($ SelectItem {:data-test-id "borrowable"
                         :value true}
             (t "pool.models.filters.borrowable.borrowable"))
          ($ SelectItem {:data-test-id "not_borrowable"
                         :value false}
             (t "pool.models.filters.borrowable.not_borrowable"))))))

(def BorrowableFilter
  (uix/as-react
   (fn [props]
     (main props))))

