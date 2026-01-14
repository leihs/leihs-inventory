(ns leihs.inventory.client.routes.pools.inventory.list.components.filters.search-filter
  (:require
   ["@@/input" :refer [Input]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router]
   [leihs.inventory.client.lib.hooks :as hooks]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [class-name]}]
  (let [ref (uix/use-ref nil)
        [search-params set-search-params!] (router/useSearchParams)
        [t] (useTranslation)
        [search set-search!] (uix/use-state (or (.get search-params "search") ""))
        debounced-search (hooks/use-debounce search 300)]

    (uix/use-effect
     (fn []
       (cond
         (and (= debounced-search "")
              (.has search-params "search")
              (not= "" (.. search-params (get "search"))))
         (do
           (.delete search-params "search")
           (.set search-params "page" "1")
           (set-search-params! search-params))

         (and (not= debounced-search "")
              (not= debounced-search (.. search-params (get "search"))))
         (do
           (.set search-params "page" "1")
           (.set search-params "search" debounced-search)
           (set-search-params! search-params))))
     [debounced-search search-params set-search-params!])

    (uix/use-effect
     (fn []
       (let [on-key-down
             (fn [e]
               (when (and (= (.. e -code) "KeyF")
                          (.-altKey e)
                          (.-shiftKey e)
                          (not (.-ctrlKey e))
                          (not (.-metaKey e)))
                 (.preventDefault e)
                 (when ref
                   (when-let [input-element (.-current ref)]
                     (.focus input-element)))))]

         (js/window.addEventListener "keydown" on-key-down)
         (fn [] (js/window.removeEventListener "keydown" on-key-down))))
     [])

    ($ Input {:ref ref
              :placeholder (t "pool.models.filters.search.placeholder")
              :name "search"
              :className (str "w-48 py-0" class-name)
              :value search
              :onChange #(set-search! (.. % -target -value))})))

(def SearchFilter
  (uix/as-react
   (fn [props]
     (main props))))

