(ns leihs.inventory.client.routes.pools.inventory.list.components.filters.borrowable-filter
  (:require
   ["@@/select" :refer [Select SelectContent SelectItem SelectTrigger
                        SelectValue]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [className]}]
  (let [[search-params set-search-params!] (router/useSearchParams)
        type (.. search-params (get "type"))
        [t] (useTranslation)

        borrowable (js/JSON.parse (.. search-params (get "borrowable")))
        handle-borrowable (fn [value]
                            (if (= value nil)
                              (.delete search-params "borrowable")
                              (.set search-params "borrowable" value))
                            (.set search-params "page" "1")
                            (set-search-params! search-params))]

    ($ Select {:value borrowable
               :disabled (= type "option")
               :onValueChange handle-borrowable}
       ($ SelectTrigger {:name "borrowable"
                         :className (str "w-[260px]" className)}
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

