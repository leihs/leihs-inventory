(ns leihs.inventory.client.components.form.autocomplete
  (:require
   ["@/components/ui/command" :refer [Command CommandEmpty
                                      CommandInput CommandItem CommandList]]
   ["@/components/ui/popover" :refer [Popover PopoverContent PopoverTrigger]]
   ["@@/button" :refer [Button]]
   ["@@/form" :refer [FormField FormItem FormLabel
                      FormControl FormMessage]]
   ["@@/spinner" :refer [Spinner]]
   ["lucide-react" :refer [Check ChevronsUpDown]]
   ["react-i18next" :refer [useTranslation]]
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui main [{:keys [form name label props]}]
  (let [[t] (useTranslation)
        [open set-open!] (uix/use-state false)
        [width set-width!] (uix/use-state nil)

        control (cj (.-control form))
        buttonRef (uix/use-ref nil)
        debounceTimerRef (uix/use-ref nil)
        instant? (boolean (:instant props))
        values-url (:values-url props)
        remap (if values-url (:remap props) nil)
        disabled (:disabled props)

        set-value (aget form "setValue")
        get-values (aget form "getValues")

        ;; State for dynamically fetched options
        [options set-options!] (uix/use-state (if values-url [] (:options props)))
        [loading? set-loading!] (uix/use-state false)

        get-label (fn [value]
                    (some (fn [opt]
                            (when (= (:value opt) value)
                              (:label opt)))
                          options))

        handle-select (fn [value]
                        (set-open! false)
                        (js/console.debug name)
                        (set-value name #js {:value value
                                             :label (get-label value)}
                                   #js {:shouldDirty true
                                        :shouldValidate true})
                        (when instant?
                          (set-options! [])))

        handle-search (fn [event]
                        (let [value (.. event -target -value)]
                          ;; Clear existing timeout
                          (when @debounceTimerRef
                            (js/clearTimeout @debounceTimerRef))

                          (when (and instant? (> (count value) 3))
                            ;; Set new timeout for debouncing (300ms delay)
                            (reset! debounceTimerRef
                                    (js/setTimeout
                                     (fn []
                                       (set-loading! true)
                                       (-> http-client
                                           (.get (str values-url value))
                                           (.then (fn [response]
                                                    (let [data (jc (.. response -data))]
                                                      (if remap
                                                        (set-options! (map remap data))
                                                        (set-options! data))
                                                      (set-loading! false))))))
                                     300)))))

        handle-open-change (fn [val] (set-open! val))]

    (uix/use-effect
     (fn []
       (when (.. buttonRef -current)
         (set-width! (.. buttonRef -current -offsetWidth))))
     [])

    (uix/use-effect
     (fn []
       (when (and (not instant?) values-url)
         (set-loading! true)
         (-> http-client
             (.get values-url)
             (.then (fn [response]
                      (let [data (jc (.. response -data))]
                        (if remap
                          (set-options! (map remap data))
                          (set-options! data))
                        (set-loading! false)))))))
     [values-url remap instant?])

    ($ FormField
       {:control control
        :name name
        :render #($ FormItem {:class-name "mt-6"}
                    ($ FormLabel label)
                    ($ Popover {:open open
                                :on-open-change handle-open-change}

                       ($ PopoverTrigger {:as-child true}
                          ($ FormControl
                             ($ Button {:ref buttonRef
                                        :variant "outline"
                                        :disabled disabled
                                        :name name
                                        :data-test-id name
                                        :role "combobox"
                                        :class-name "w-full justify-between"}
                                (or (get-values (str name ".label"))
                                    (t "pool.items.item.fields.autocomplete.select"))
                                ($ ChevronsUpDown {:class-name "ml-2 h-4 w-4 shrink-0 opacity-50"}))))

                       ($ PopoverContent {:class-name "p-0"
                                          :style {:width (str width "px")}}

                          ($ Command {:on-change handle-search}
                             ($ CommandInput {:placeholder (t "pool.items.item.fields.autocomplete.search")
                                              :data-test-id (str name "-input")})
                             ($ CommandList
                                (if loading?
                                  ($ Spinner {:className "absolute right-0 top-0 m-3"})
                                  ($ CommandEmpty (t "pool.items.item.fields.autocomplete.not_found")))
                                (for [option options]
                                  ($ CommandItem {:value (:value option)
                                                  :onSelect handle-select
                                                  :keywords #js [(:label option)]
                                                  :key (:value option)}

                                     ($ Check
                                        {:class-name (str "mr-2 h-4 w-4 "
                                                          (if (= (:value option)
                                                                 (get-values (str name ".value")))
                                                            "visible"
                                                            "invisible"))})
                                     ($ :button {:type "button"}
                                        (:label option))))))))
                    ($ FormMessage))})))

(def Autocomplete
  (uix/as-react
   (fn [props]
     (main props))))
