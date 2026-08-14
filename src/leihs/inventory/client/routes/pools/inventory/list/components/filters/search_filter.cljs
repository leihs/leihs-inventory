(ns leihs.inventory.client.routes.pools.inventory.list.components.filters.search-filter
  (:require
   ["@@/input" :refer [Input]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router" :as router]
   [leihs.inventory.client.lib.hooks :as hooks]
   [uix.core :as uix :refer [$ defui]]))

(defui SearchFilter [{:keys [class-name]}]
  (let [ref (uix/use-ref nil)
        [search-params set-search-params!] (router/useSearchParams)
        [t] (useTranslation)
        [search set-search!] (uix/use-state (or (.get search-params "search") ""))
        [debounced-search reset-debounce!] (hooks/use-debounce search 300)
        prev-url-search (uix/use-ref (.get search-params "search"))]

    (uix/use-effect
     (fn []
       (let [prev @prev-url-search
             current (.get search-params "search")]
         (reset! prev-url-search current)

         ;; when search query param was in URL before (?search -> nil)
         ;; reset the debounced timer and search
         (if (and (some? prev)
                  (nil? current))

           ;; External removal of search query param: 
           ;; cancel pending debounce and reset input.
           (do (set-search! "")
               (reset-debounce! ""))

           ;; otherwise when search query param wasn't in URL and now is (nil -> ?search)
           ;; Guard ensures we only update the URL once the debounce has settled.
           (when (= debounced-search search)
             (cond
               (and (= debounced-search "") (.has search-params "search"))
               (do (.delete search-params "search")
                   (.set search-params "page" "1")
                   (set-search-params! search-params))

               (and (not= debounced-search "") (not= debounced-search current))
               (do (.set search-params "page" "1")
                   (.set search-params "search" debounced-search)
                   (set-search-params! search-params)))))))
     [debounced-search search search-params
      set-search-params! reset-debounce!])

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
                   (when-let [input-element @ref]
                     (.focus input-element)))))]

         (js/window.addEventListener "keydown" on-key-down)
         (fn [] (js/window.removeEventListener "keydown" on-key-down))))
     [])

    ($ Input {:ref ref
              :placeholder (t "pool.models.filters.search.placeholder")
              :name "search"
              :class-name (str "w-48 py-0" class-name)
              :value search
              :onChange #(set-search! (.. % -target -value))})))
