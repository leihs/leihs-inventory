(ns leihs.inventory.client.routes.pools.inventory.list.components.filters.search-filter
  (:require
   ["@@/input" :refer [Input]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [class-name]}]
  (let [ref (uix/use-ref nil)
        [search-params set-search-params!] (router/useSearchParams)
        [t] (useTranslation)
        [search set-search!] (uix/use-state (or (.get search-params "search") ""))
        [interacting set-interacting!] (uix/use-state false)]

    (uix/use-effect
     (fn []
       (when (and (empty? (.. search-params (get "search")))
                  (not interacting))

         (set-interacting! false)
         (set-search! "")))

     [search-params search search-params interacting])

    (uix/use-effect
     (fn []
       (when (not= search "")
         (let [debounce (js/setTimeout
                         (fn []
                           (when (not= search (.. search-params (get "search")))
                             (.set search-params "page" "1")
                             (.set search-params "search" search)

                             (set-search-params! search-params)))
                         100)]

           (fn [] (js/clearTimeout debounce)))))
     [search search-params set-search-params!])

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
              :onFocus #(set-interacting! true)
              :onBlur #(set-interacting! false)
              :onChange #(let [val (.. % -target -value)]
                           (if (= val "")
                             (do
                               (set-search! "")
                               (.delete search-params "search")
                               (set-search-params! search-params))
                             (set-search! (.. % -target -value))))})))

(def SearchFilter
  (uix/as-react
   (fn [props]
     (main props))))

