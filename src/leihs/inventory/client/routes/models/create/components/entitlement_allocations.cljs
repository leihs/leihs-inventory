(ns leihs.inventory.client.routes.models.create.components.entitlement-allocations
  (:require
   ["@/components/ui/command" :refer [Command CommandEmpty CommandGroup
                                      CommandInput CommandItem CommandList]]
   ["@/components/ui/popover" :refer [Popover PopoverContent PopoverTrigger]]
   ["@@/button" :refer [Button]]
   ["@@/form" :refer [FormControl FormField FormItem]]
   ["@@/input" :refer [Input]]
   ["@@/label" :refer [Label]]
   ["@@/table" :refer [Table TableBody TableCell TableRow]]
   ["lucide-react" :refer [ChevronsUpDown Trash]]
   ["react-hook-form" :as hook-form]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.models.create.context :refer [state-context]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defn find-name-by-id [vec id]
  (some #(when (= (:id %) id) (:name %)) vec))

(defui main [{:keys [control items form props]}]
  (let [{:keys [entitlements]} (uix/use-context state-context)
        [allocations set-allocations!] (uix/use-state 0)
        [width set-width!] (uix/use-state nil)
        buttonRef (uix/use-ref nil)
        set-value (aget form "setValue")
        get-values (aget form "getValues")

        {:keys [fields append remove]} (jc (hook-form/useFieldArray
                                            (cj {:control control
                                                 :name "entitlements"})))
        handle-quantity-change
        (fn [index val]

          (set-value (str "entitlements" index ".quantity")
                     val
                     (cj {:shouldValidate true
                          :shouldDirty true
                          :shouldTouch true})))]

    (uix/use-effect
     (fn []
       (when (.. buttonRef -current)
         (set-width! (.. buttonRef -current -offsetWidth))))
     [])

    (uix/use-effect
     (fn []
       (let [entitlements (jc (get-values "entitlements"))
             allocations-combined (reduce (fn [acc item]
                                            (+ acc (js/parseInt (:quantity item))))
                                          0
                                          entitlements)]

         (set-allocations! allocations-combined)))
     [fields get-values allocations])

    ($ :div {:class-name "flex flex-col gap-2"}
       ($ Label "Zuteilungen (max. " (str items) ")")

       ($ Popover
          ($ PopoverTrigger {:as-child true}
             ($ Button {:ref buttonRef
                        :variant "outline"
                        :role "combobox"
                        :class-name (str "justify-between w-full")}
                "Select Entitlement Group"
                ($ ChevronsUpDown {:class-name "ml-2 h-4 w-4 shrink-0 opacity-50"})))

          ($ PopoverContent {:class-name "p-0"
                             :style {:width (str width "px")}}
             ($ Command
                ($ CommandInput {:placeholder "Search entitlement groups"})
                ($ CommandList
                   ($ CommandEmpty "No Entitlement Group Found")
                   ($ CommandGroup
                      (for [entitlement entitlements]
                        ($ CommandItem {:value (:name entitlement)
                                        :onSelect #(append (cj {:entitlement_group_id (:id entitlement)
                                                                :entitlement_id nil
                                                                :quantity "0"}))
                                        :key (:id entitlement)}
                           (:name entitlement))))))))

       (when (not-empty fields)
         ($ :div {:class-name "rounded-md border overflow-hidden"}
            ($ Table {:class-name "w-full"}
               ($ TableBody

                  (doall
                   (map-indexed
                    (fn [index field]
                      ($ TableRow {:class-name "" :key (:id field)}

                         ($ TableCell {:class-name (str "w-4 h-full p-0"
                                                        (if (> (+ (js/parseInt items) 1)
                                                               (js/parseInt allocations))
                                                          " bg-green-500"
                                                          " bg-red-500"))})

                         ($ TableCell {:class-name "hidden"}
                            ($ FormField
                               {:control (cj control)
                                :name (str "entitlements." index ".entitlement_group_id")
                                :render #($ FormItem
                                            ($ FormControl
                                               ($ Input (merge
                                                         {:className ""}
                                                         (:field (jc %))))))}))

                         ($ TableCell {:class-name "w-[70%]"}
                            (find-name-by-id
                             entitlements
                             (:entitlement_group_id field)))

                         ($ TableCell
                            ($ FormField
                               {:control (cj control)
                                :name (str "entitlements." index ".quantity")
                                :render #($ FormItem
                                            ($ FormControl
                                               ($ Input (merge
                                                         {:className ""
                                                          :type "number"
                                                          :min "0"
                                                          :on-input (fn [ev]
                                                                      (handle-quantity-change
                                                                       index
                                                                       (.. ev -target -value)))}
                                                         (:field (jc %))))))}))

                         ($ TableCell {:class-name "flex gap-2 justify-end"}
                            ($ Button {:variant "outline"
                                       :type "button"
                                       :on-click #(remove index)
                                       :size "icon"}
                               ($ Trash {:class-name "h-4 w-4"})))))
                    fields)))))))))

(def EntitlementAllocations
  (uix/as-react
   (fn [props]
     (main props))))
