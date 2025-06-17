(ns leihs.inventory.client.routes.models.components.before-last-check-filter
  (:require
   ["@@/button" :refer [Button]]
   ["@@/calendar" :refer [Calendar]]
   ["@@/popover" :refer [Popover PopoverContent PopoverTrigger]]
   ["date-fns" :as date-fns]
   ["lucide-react" :refer [CalendarDays]]
   ["react-i18next" :refer [useTranslation]]
   [leihs.inventory.client.routes.models.filter-reducer :as filter-reducer]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [class-name]}]
  (let [search-params (js/URLSearchParams. (.. js/window -location -search))

        dispatch (filter-reducer/use-filter-dispatcher)
        state (filter-reducer/use-filter-state)

        [t] (useTranslation)
        before-last-check (.get search-params "before_last_check")
        handle-before-last-check (fn [date]
                                   (let [formatted-date (if date
                                                          (date-fns/format date "yyyy-MM-dd")
                                                          nil)]
                                     (if (or (= date nil) (= formatted-date before-last-check))
                                       (dispatch {:filter "before_last_check" :value formatted-date :delete true})
                                       (dispatch {:filter "before_last_check" :value formatted-date}))))]

    ($ Popover
       ($ PopoverTrigger {:asChild true}
          ($ Button {:variant "outline"
                     :disabled (some #{:before_last_check} state)
                     :name "before-last-check-filter"}
             ($ CalendarDays {:className (str "h-4 w-4 mr-2" class-name)})
             (t "pool.models.filters.before_last_check.title")))

       ($ PopoverContent {:className "w-[280px]"}
          ($ Calendar {:mode "single"
                       :selected (js/Date. before-last-check)
                       :onSelect handle-before-last-check})))))

(def BeforeLastCheckFilter
  (uix/as-react
   (fn [props]
     (main props))))

