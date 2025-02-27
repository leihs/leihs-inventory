(ns leihs.inventory.client.routes.models.components.forms.manufacturer
  (:require
   ["@/components/ui/command" :refer [Command CommandEmpty CommandInput
                                      CommandItem CommandList]]
   ["@/components/ui/popover" :refer [Popover PopoverContent PopoverTrigger]]
   ["@@/button" :refer [Button]]
   ["@@/form" :refer [FormControl FormField FormItem FormLabel]]
   ["lucide-react" :refer [ChevronsUpDown]]
   [clojure.string :as str]
   [leihs.inventory.client.lib.utils :refer [cj]]
   [leihs.inventory.client.routes.models.create.context :refer [state-context]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui main [{:keys [form control props]}]
  (let [{:keys [manufacturers]} (uix/use-context state-context)
        [open set-open!] (uix/use-state false)
        [width set-width!] (uix/use-state nil)
        set-value (aget form "setValue")
        buttonRef (uix/use-ref nil)]

    (uix/use-effect
     (fn []
       (when (.. buttonRef -current)
         (set-width! (.. buttonRef -current -offsetWidth))))
     [])

    ($ :div {:class-name "flex flex-col gap-2"}
       ($ FormField
          {:name "manufacturer"
           :control (cj control)
           :render #($ FormItem {:class-name "flex flex-col mt-6"}
                       ($ FormLabel "Manufacturer")
                       ($ Popover {:open open
                                   :on-open-change (fn [v] (set-open! v))}
                          ($ PopoverTrigger {:as-child true}
                             ($ FormControl
                                ($ Button {:variant "outline"
                                           :role "combobox"
                                           :ref buttonRef
                                           :on-click (fn [] (set-open! (not open)))
                                           :class-name "w-full justify-between"}

                                   (if (str/blank? (str (.. ^js % -field -value)))
                                     "Select manufacturer"
                                     (.. ^js % -field -value))

                                   ($ ChevronsUpDown {:class-name "ml-2 h-4 w-4 shrink-0 opacity-50"}))))

                          ($ PopoverContent {:class-name "p-0"
                                             :style {:width (str width "px")}}
                             ($ Command
                                {:filter (fn [value search]
                                           (let [lSearch (str/lower-case search)
                                                 lValue (str/lower-case value)]
                                             (if (str/includes? lValue lSearch) 1 0)))}
                                ($ CommandInput {:placeholder "Search item..."})
                                ($ CommandList

                                   ($ CommandEmpty "No item found.")

                                   (for [manufacturer manufacturers]
                                     ($ CommandItem {:key manufacturer
                                                     :value manufacturer
                                                     :on-select (fn []
                                                                  (set-open! false)
                                                                  (set-value "manufacturer" manufacturer))}

                                        ($ :span manufacturer))))))))}))))

(def Manufacturer
  (uix/as-react
   (fn [props]
     (main props))))
