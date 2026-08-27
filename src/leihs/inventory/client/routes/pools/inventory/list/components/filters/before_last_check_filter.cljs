(ns leihs.inventory.client.routes.pools.inventory.list.components.filters.before-last-check-filter
  (:require
   ["@@/button" :refer [Button]]
   ["@@/calendar" :refer [Calendar]]
   ["@@/popover" :refer [Popover PopoverContent PopoverTrigger]]
   ["date-fns" :as date-fns]
   ["lucide-react" :refer [CalendarDays ChevronsUpDown]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router" :as router]
   [leihs.inventory.client.lib.date-utils :refer [date-from-iso]]
   [uix.core :as uix :refer [$ defui]]))

(defui BeforeLastCheckFilter [{:keys [class-name]}]
  (let [[search-params set-search-params!] (router/useSearchParams)
        type (.. search-params (get "type"))
        [open set-open!] (uix/use-state false)
        [t] (useTranslation)
        before-last-check (.. search-params (get "before_last_check"))
        selected-date (date-from-iso before-last-check)
        handle-before-last-check (fn [date]
                                   (set-open! false)
                                   (let [iso-date (if date
                                                    (date-fns/format date "yyyy-MM-dd")
                                                    nil)]
                                     (if (or (= date nil) (= iso-date before-last-check))
                                       (.delete search-params "before_last_check")
                                       (.set search-params "before_last_check" iso-date))

                                     (.set search-params "page" "1")
                                     (set-search-params! search-params)))]

    ($ Popover {:open open
                :on-open-change set-open!}
       ($ PopoverTrigger {:asChild true}
          ($ Button {:variant "outline"
                     :class-name (str "min-w-48 max-w-48 " class-name)
                     :data-test-id "before-last-check-filter-button"
                     :disabled (or (= type "software")
                                   (= type "option"))}

             ($ CalendarDays {:class-name "h-4 w-4"})
             (if selected-date
               ($ :span {:class-name "truncate w-full text-left"}
                  (t "intlDateTime" #js {:val selected-date}))
               (t "pool.models.filters.before_last_check.title"))
             ($ ChevronsUpDown {:class-name "ml-auto h-4 w-4 shrink-0 opacity-50"})))

       ($ PopoverContent {:class-name "w-[280px]"}
          ($ Calendar {:mode "single"
                       :captionLayout "dropdown"
                       :data-test-id "before-last-check-calendar"
                       :selected selected-date
                       :defaultMonth selected-date
                       :onSelect handle-before-last-check})))))



