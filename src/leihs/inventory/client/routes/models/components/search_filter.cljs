(ns leihs.inventory.client.routes.models.components.search-filter
  (:require
   ["@@/input" :refer [Input]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [class-name]}]
  (let [[search-params set-search-params!] (router/useSearchParams)
        [t] (useTranslation)
        search (or (.get search-params "search") "")
        handle-search (fn [e]
                        (let [value (.. e -target -value)]
                          (when (not= value "")
                            (.set search-params "page" "1"))
                          (if (= value "")
                            (.delete search-params "search")
                            (.set search-params "search" value))
                          (set-search-params! search-params)))]

    ($ Input {:placeholder (t "pool.models.filters.search.placeholder")
              :name "search"
              :className (str "w-fit py-0" class-name)
              :value search
              :onChange handle-search})))

(def SearchFilter
  (uix/as-react
   (fn [props]
     (main props))))

