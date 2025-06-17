(ns leihs.inventory.client.routes.models.components.borrowable-filter
  (:require
   ["@@/select" :refer [Select SelectContent SelectItem SelectTrigger
                        SelectValue]]
   ["react-i18next" :refer [useTranslation]]
   [leihs.inventory.client.routes.models.filter-reducer :as filter-reducer]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [class-name]}]
  (let [search-params (js/URLSearchParams. (.. js/window -location -search))
        [t] (useTranslation)

        dispatch (filter-reducer/use-filter-dispatcher)
        state (filter-reducer/use-filter-state)

        borrowable (js/JSON.parse (.. search-params (get "borrowable")))

        handle-borrowable (fn [value]
                            (if (= value nil)
                              (dispatch {:filter "borrowable" :value nil :delete true})
                              (dispatch {:filter "borrowable" :value value})))]

    ($ Select {:value borrowable
               :onValueChange handle-borrowable}
       ($ SelectTrigger {:name "borrowable"
                         :disabled (some #{:borrowable} state)
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

