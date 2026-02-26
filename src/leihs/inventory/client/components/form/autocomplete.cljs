(ns leihs.inventory.client.components.form.autocomplete
  (:require
   ["@/components/ui/command" :refer [Command CommandEmpty CommandInput
                                      CommandItem CommandList CommandSeparator]]
   ["@/components/ui/popover" :refer [Popover PopoverContent PopoverTrigger]]
   ["@@/button" :refer [Button]]
   ["@@/form" :refer [FormControl FormField FormItem FormLabel FormMessage]]
   ["@@/spinner" :refer [Spinner]]
   ["lucide-react" :refer [Check ChevronsUpDown FilePlusCorner]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router]
   [leihs.inventory.client.lib.client :refer [http-client safe-concat]]
   [leihs.inventory.client.lib.hooks :as hooks]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui main [{:keys [form name label props]}]
  (let [[t] (useTranslation)
        [open set-open!] (uix/use-state false)
        [width set-width!] (uix/use-state nil)

        params (router/useParams)

        control (cj (.-control form))
        buttonRef (uix/use-ref nil)

        ;; enable instant search after 2 characters
        instant? (boolean (:instant props))
        ;; enable adding new entries if no existing option matches the search
        extendable? (boolean (:extendable props))
        ;; interpolate values-url with params /:pool-id/.. -> /42/.. etc.
        interpolate? (boolean (:interpolate props))

        values-url (if interpolate?
                     (router/generatePath (:values-url props) params)
                     (:values-url props))

        ;; remap function to transform fetched values
        remap (if values-url (:remap props) nil)
        disabled (:disabled props)

        [search set-search!] (uix/use-state "")
        debounced-search (hooks/use-debounce search 300)

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
                        (let [label (get-label value)]
                          (set-value name #js {:value value
                                               :label (if label label value)}
                                     #js {:shouldDirty true
                                          :shouldValidate true})))

        handle-open-change (fn [val]
                             ;; Calculate width when opening popover
                             (when (and val @buttonRef)
                               (set-width! (.. buttonRef -current -offsetWidth)))
                             (when instant?
                               (set-options! []))
                             (set-search! "")
                             (set-open! val))]

    (uix/use-effect
     (fn []
       (cond
         (and instant? (< (count debounced-search) 2))
         (set-options! [])

         (and instant? (> (count debounced-search) 1))
         (let [fetch (fn []
                       (set-loading! true)
                       (-> http-client
                           (.get (safe-concat values-url debounced-search))
                           (.then (fn [response]
                                    (let [data (jc (.. response -data))]
                                      (if remap
                                        (set-options! (map remap data))
                                        (set-options! data))
                                      (set-loading! false))))))]
           (fetch))))
     [debounced-search values-url remap instant?])

    ;; initial fetch of options if not instant
    ;; when options are delivered via url
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
                    (when label
                      ($ FormLabel (t label)
                         (when (-> props :required) "*")))
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

                                ;; the value in form can either be a string or a map with label and value
                                ;; hence we check for both cases here
                                (let [val (jc (get-values (str name)))
                                      label (if (map? val) (:label val) val)]
                                  (if (and label (seq label))
                                    label
                                    (t (-> props :text :select))))

                                ($ ChevronsUpDown {:class-name "ml-2 h-4 w-4 shrink-0 opacity-50"}))))

                       ($ PopoverContent {:class-name "p-0"
                                          :style {:width (str width "px")}}

                          ($ Command {:should-filter (if instant? false true)
                                      :on-change (fn [e] (set-search! (.. e -target -value)))}
                             ($ CommandInput {:placeholder (t (-> props :text :search))
                                              :data-test-id (str name "-input")})

                             ($ CommandList
                                (if loading?
                                  ($ Spinner {:className "absolute right-0 top-0 m-3"})
                                  ($ CommandEmpty (t (-> props :text :empty))))

                                ;; extendable option to add new entry
                                ;; will only show if no existing option matches the search
                                ;; can be toggled via extendable? prop
                                (when (and extendable?
                                           (not (some (fn [opt]
                                                        (= (:label opt) search))
                                                      options))
                                           (seq search))
                                  ($ :<>
                                     ($ CommandItem {:value search
                                                     :onSelect handle-select}
                                        ($ FilePlusCorner {:class-name "mr-2 h-4 w-4 text-blue-500"})

                                        ($ :button {:type "button"
                                                    :class-name "w-full flex justify-between"}
                                           search
                                           ($ :span {:class-name "rounded-full text-blue-500 mx-2 text-xs"}
                                              (t (-> props :text :add_new)))))

                                     ($ CommandSeparator {:alwaysRender true})))

                                ;; options
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
