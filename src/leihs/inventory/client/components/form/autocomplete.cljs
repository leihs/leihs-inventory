(ns leihs.inventory.client.components.form.autocomplete
  (:require
   ["@/components/ui/command" :refer [Command CommandEmpty CommandGroup
                                      CommandInput CommandItem CommandList]]
   ["@/components/ui/popover" :refer [Popover PopoverContent PopoverTrigger]]
   ["@@/button" :refer [Button]]
   ["@@/form" :refer [FormField FormItem FormLabel FormMessage]]
   ["lucide-react" :refer [Check ChevronsUpDown]]
   ["react-i18next" :refer [useTranslation]]
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui main [{:keys [control name label props]}]
  (let [[t] (useTranslation)
        [open set-open!] (uix/use-state false)
        [width set-width!] (uix/use-state nil)
        buttonRef (uix/use-ref nil)

        values-url (:values-url props)
        remap (if values-url (:remap props) nil)
        ;; State for dynamically fetched options
        [options set-options!] (uix/use-state (if values-url [] (:options props)))
        [is-loading set-loading!] (uix/use-state false)]

    (uix/use-effect
     (fn []
       (when (.. buttonRef -current)
         (set-width! (.. buttonRef -current -offsetWidth))))
     [])

    (uix/use-effect
     (fn []
       (when values-url
         (-> http-client
             (.get values-url)
             (.then (fn [response]
                      (let [data (jc (.. response -data))]
                        (if remap
                          (set-options! (map remap data))
                          (set-options! data))))))))
     [values-url remap])

    ($ FormField
       {:control (cj control)
        :name name
        :render (fn [field-data]
                  (let [field (jc field-data)
                        current-value (-> field :field :value)
                        selected-label (some (fn [opt]
                                               (when (= (:value opt) current-value)
                                                 (:label opt)))
                                             options)]

                    ($ FormItem {:class-name "mt-6"}
                       ($ FormLabel (t label))
                       ($ Popover {:open open
                                   :on-open-change (fn [val] (set-open! val))}
                          ($ PopoverTrigger {:as-child true}
                             ($ Button {:ref buttonRef
                                        :variant "outline"
                                        :role "combobox"
                                        :class-name "w-full justify-between"}
                                (or selected-label (t "pool.item.create.autocomplete.select"))
                                ($ ChevronsUpDown {:class-name "ml-2 h-4 w-4 shrink-0 opacity-50"})))

                          ($ PopoverContent {:class-name "p-0"
                                             :style {:width (str width "px")}}
                             ($ Command
                                ($ CommandInput {:placeholder (t "pool.item.create.autocomplete.search")})
                                ($ CommandList
                                   ($ CommandEmpty (t "pool.item.create.autocomplete.not-found"))
                                   ($ CommandGroup
                                      (for [option options]
                                        ($ CommandItem {:value (:value option)
                                                        :onSelect (fn []
                                                                    (set-open! false)
                                                                    ((-> field :field :onChange) (:value option)))
                                                        :key (:value option)}

                                           ($ Check
                                              {:class-name (str "mr-2 h-4 w-4 "
                                                                (if (= (:value option) current-value)
                                                                  "visible"
                                                                  "invisible"))})
                                           (:label option))))))))
                       ($ FormMessage))))})))

(def Autocomplete
  (uix/as-react
   (fn [props]
     (main props))))
