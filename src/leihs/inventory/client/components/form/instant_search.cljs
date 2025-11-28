(ns leihs.inventory.client.components.form.instant-search
  (:require
   ["@/components/ui/popover" :refer [Popover PopoverAnchor PopoverContent]]
   ["@@/button" :refer [Button]]
   ["@@/form" :refer [FormControl FormField FormItem FormLabel]]
   ["@@/input" :refer [Input]]
   ["lucide-react" :refer [Check]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [useParams useLoaderData]]
   [clojure.string :as str]
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui main [{:keys [form name label props]}]
  (let [[t] (useTranslation)
        [open set-open!] (uix/use-state false)
        [width set-width!] (uix/use-state nil)
        control (cj (.-control form))

        params (useParams)
        path (router/generatePath (:resource props) params)

        get-values (aget form "getValues")
        set-value (aget form "setValue")

        input-ref (uix/use-ref nil)
        list-ref (uix/use-ref nil)

        [search set-search!] (uix/use-state (get-values name))
        [result set-result!] (uix/use-state [])

        handle-key-down (fn [e]
                          (let [key (.. e -key)]

                            (cond
                              (= key "Enter")
                              (do
                                (.preventDefault e)
                                (.stopPropagation e)
                                (when open (set-open! false)))

                              (= key "ArrowDown")
                              (do
                                (.preventDefault e)
                                (some-> list-ref
                                        .-current
                                        .-firstChild
                                        .-firstChild
                                        (.focus)))

                              (and (= key "Tab") open)
                              (do
                                (.preventDefault e)
                                (some-> list-ref
                                        .-current
                                        .-firstChild
                                        .-firstChild
                                        (.focus)))

                              :else
                              nil)))

        handle-key-down-list (fn [e]
                               (let [key (.. e -key)]
                                 (cond
                                   (= key "ArrowUp")
                                   (do
                                     (.preventDefault e)
                                     (some-> e
                                             .-target
                                             .-parentElement
                                             .-previousElementSibling
                                             .-firstChild
                                             (.focus)))

                                   (= key "ArrowDown")
                                   (do
                                     (.preventDefault e)
                                     (some-> e
                                             .-target
                                             .-parentElement
                                             .-nextElementSibling
                                             .-firstChild
                                             (.focus)))

                                   :else
                                   nil)))]

    (uix/use-effect
     (fn []
       (when (= (.. js/document -activeElement -id)
                (.. input-ref -current -firstChild -id))

         (if (< (count search) 3)
           (set-open! false)

           (do
             (when (= search "")
               (set-open! false))

             (when (and (not= search "")
                        (not= search (get-values name)))

               (set-value name search)

               (let [uri (str path search)
                     debounce (js/setTimeout
                               (fn []
                                 (set-open! true)
                                 ;; Fetch result based on the search term
                                 (-> http-client
                                     (.get (str path (js/encodeURIComponent search)))
                                     (.then (fn [res]
                                              (let [data (jc (.-data res))]
                                                (set-result! data))))
                                     (.catch
                                      (fn [err]
                                        (js/console.error "Error fetching result" err)))))
                               200)]

                 (fn [] (js/clearTimeout debounce))))))))
     [search props get-values set-value path name])

    (uix/use-effect
     (fn []
       (when (.. input-ref -current)
         (set-width! (.. input-ref -current -offsetWidth))))
     [])

    ($ :div {:class-name "flex flex-col gap-2"}
       ($ FormField
          {:name name
           :control control
           :render #($ FormItem {:class-name "flex flex-col mt-6"}
                       ($ FormLabel (t label))

                       ($ Popover {:open open}
                          ($ PopoverAnchor {:asChild true}
                             ($ :div {:ref input-ref}
                                ($ FormControl
                                   ($ Input (merge (:field (jc %))
                                                   {:auto-complete "off"
                                                    :on-key-down handle-key-down
                                                    :value search
                                                    :on-change (fn [e] (set-search! (-> e .-target .-value)))})))))

                          ($ PopoverContent {:class-name "p-0"
                                             :on-key-down handle-key-down-list
                                             :onOpenAutoFocus (fn [e] (.. e (preventDefault)))
                                             :onCloseAutoFocus (fn [_] (some-> input-ref
                                                                               .-current
                                                                               .-firstChild
                                                                               (.focus)))

                                             :onInteractOutside (fn [_]
                                                                  (set-open! false)
                                                                  (some-> input-ref
                                                                          .-current
                                                                          .-firstChild
                                                                          (.focus)))
                                             :onEscapeKeyDown (fn []
                                                                (set-open! false)
                                                                (some-> input-ref
                                                                        .-current
                                                                        .-firstChild
                                                                        (.focus)))
                                             :style {:width (str width "px")}}

                             ($ :ul {:class-name "max-h-60 overflow-y-auto overflow-x-hidden p-2"
                                     :ref list-ref}
                                (if (empty? result)
                                  ($ :li {:class-name "text-muted-foreground text-center text-sm"}
                                     ($ :span (t (:not-found props))))
                                  (for [current result]
                                    (if (map? current)
                                      (let [name (:name current)
                                            id (:id current)]
                                        ($ :li {:key id}
                                           ($ Button {:variant "ghost"
                                                      :class-name "w-full justify-start"
                                                      :on-click (fn []
                                                                  (set-open! false)
                                                                  (set-search! name)
                                                                  (set-value name id))}
                                              ($ Check
                                                 {:class-name (str "mr-2 h-4 w-4 "
                                                                   (if (= id (get-values name))
                                                                     "visible"
                                                                     "invisible"))})
                                              ($ :span name))))

                                      ($ :li {:key current}
                                         ($ Button {:variant "ghost"
                                                    :class-name "w-full justify-start"
                                                    :on-click (fn []
                                                                (set-open! false)
                                                                (set-search! current)
                                                                (set-value name current))}

                                            ($ Check
                                               {:class-name (str "mr-2 h-4 w-4 "
                                                                 (if (= current search)
                                                                   "visible"
                                                                   "invisible"))})
                                            ($ :span current))))))))))}))))

(def InstantSearch
  (uix/as-react
   (fn [props]
     (main props))))
