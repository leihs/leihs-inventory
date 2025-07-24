(ns leihs.inventory.client.routes.models.components.before-last-check-filter
  (:require
   ["@@/button" :refer [Button]]
   ["@@/calendar" :refer [Calendar]]
   ["@@/popover" :refer [Popover PopoverContent PopoverTrigger]]
   ["date-fns" :as date-fns]

   ["lucide-react" :refer [CalendarDays]]
   ["react-i18next" :refer [useTranslation]]

   ["react-router-dom" :as router]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [class-name]}]
  (let [[search-params set-search-params!] (router/useSearchParams)
        [t] (useTranslation)
        before-last-check (.. search-params (get "before_last_check"))
        handle-before-last-check (fn [date]
                                   (let [formatted-date (if date
                                                          (date-fns/format date "yyyy-MM-dd")
                                                          nil)]
                                     (if (or (= date nil) (= formatted-date before-last-check))
                                       (.delete search-params "before_last_check")
                                       (.set search-params "before_last_check" formatted-date))

                                     (.set search-params "page" "1")
                                     (set-search-params! search-params)))]

    ($ Popover
       ($ PopoverTrigger {:asChild true}
          ($ Button {:variant "outline"}
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

