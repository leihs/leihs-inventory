(ns leihs.inventory.client.routes.models.components.retired-filter
  (:require
   ["@@/select" :refer [Select SelectContent SelectItem SelectTrigger
                        SelectValue]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router]
   [leihs.inventory.client.routes.models.filter-reducer :as filter-reducer]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [class-name]}]
  (let [search-params (js/URLSearchParams. (.. js/window -location -search))
        [t] (useTranslation)

        dispatch (filter-reducer/use-filter-dispatcher)
        state (filter-reducer/use-filter-state)

        retired (js/JSON.parse (.. search-params (get "retired")))
        handle-retired (fn [value]
                         (if (= value nil)
                           (dispatch {:filter "retired" :value nil :delete true})
                           (dispatch {:filter "retired" :value value})))]

    ($ Select {:value retired
               :onValueChange handle-retired}
       ($ SelectTrigger {:name "retired"
                         :disabled (some #{:retired} state)
                         :className (str "w-[280px] " class-name)}
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

