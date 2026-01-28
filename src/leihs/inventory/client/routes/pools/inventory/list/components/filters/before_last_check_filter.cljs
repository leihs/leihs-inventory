(ns leihs.inventory.client.routes.pools.inventory.list.components.filters.before-last-check-filter
  (:require
   ["@@/button" :refer [Button]]
   ["@@/calendar" :refer [Calendar]]
   ["@@/popover" :refer [Popover PopoverContent PopoverTrigger]]
   ["date-fns" :as date-fns]

   ["lucide-react" :refer [CalendarDays ChevronsUpDown]]
   ["react-i18next" :refer [useTranslation]]

   ["react-router-dom" :as router]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [className]}]
  (let [[search-params set-search-params!] (router/useSearchParams)
        type (.. search-params (get "type"))
        [open set-open!] (uix/use-state false)
        [t] (useTranslation)
        before-last-check (.. search-params (get "before_last_check"))
        handle-before-last-check (fn [date]
                                   (set-open! false)
                                   (let [formatted-date (if date
                                                          (date-fns/format date "yyyy-MM-dd")
                                                          nil)]
                                     (if (or (= date nil) (= formatted-date before-last-check))
                                       (.delete search-params "before_last_check")
                                       (.set search-params "before_last_check" formatted-date))

                                     (.set search-params "page" "1")
                                     (set-search-params! search-params)))]

    ($ Popover {:open open
                :on-open-change set-open!}
       ($ PopoverTrigger {:asChild true}
          ($ Button {:variant "outline"
                     :className (str "min-w-48 max-w-48 " className)
                     :data-test-id "before-last-check-filter-button"
                     :disabled (or (= type "software")
                                   (= type "option"))}

             ($ CalendarDays {:className "h-4 w-4"})
             (if before-last-check
               ($ :span {:class-name "truncate w-full text-left"}
                  (str before-last-check))
               (t "pool.models.filters.before_last_check.title"))
             ($ ChevronsUpDown {:class-name "ml-auto h-4 w-4 shrink-0 opacity-50"})))

       ($ PopoverContent {:className "w-[280px]"}
          ($ Calendar {:mode "single"
                       :data-test-id "before-last-check-calendar"
                       :selected (js/Date. before-last-check)
                       :onSelect handle-before-last-check})))))

(def BeforeLastCheckFilter
  (uix/as-react
   (fn [props]
     (main props))))

