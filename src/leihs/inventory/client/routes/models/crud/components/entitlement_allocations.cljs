(ns leihs.inventory.client.routes.models.crud.components.entitlement-allocations
  (:require
   ["@/components/ui/command" :refer [Command CommandEmpty CommandGroup
                                      CommandInput CommandItem CommandList]]
   ["@/components/ui/popover" :refer [Popover PopoverContent PopoverTrigger]]
   ["@@/button" :refer [Button]]
   ["@@/form" :refer [FormControl FormField FormItem]]
   ["@@/input" :refer [Input]]
   ["@@/label" :refer [Label]]
   ["@@/table" :refer [Table TableBody TableCell TableRow]]
   ["lucide-react" :refer [Check ChevronsUpDown Trash]]
   ["react-hook-form" :as hook-form]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :refer [useLoaderData]]
   [clojure.string :as str]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defn find-name-by-id [vec id]
  (some #(when (= (:id %) id) (:name %)) vec))

(defn check-path-existing [entitlement items]
  (some (fn [item]
          (= entitlement (:group_id item)))
        items))

(defn find-index-from-path [path items]
  (some (fn [[idx item]]
          (when (= path item)
            idx))
        (map-indexed vector items)))

(defui main [{:keys [control items form props]}]
  (let [{:keys [entitlement-groups]} (useLoaderData)
        [t] (useTranslation)
        [allocations set-allocations!] (uix/use-state 0)
        [width set-width!] (uix/use-state nil)
        [open set-open!] (uix/use-state false)
        buttonRef (uix/use-ref nil)
        set-value (aget form "setValue")
        get-values (aget form "getValues")

        {:keys [fields append remove]} (jc (hook-form/useFieldArray
                                            (cj {:control control
                                                 :name "entitlements"})))
        handle-quantity-change
        (fn [index val]
          (set-value (str "entitlements." index ".quantity") val))]

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
       ($ Label (t "pool.model.entitlements.blocks.entitlements.label" #js {:amount (str items)}))

       ($ Popover {:open open
                   :on-open-change #(set-open! %)}
          ($ PopoverTrigger {:as-child true}
             ($ Button {:ref buttonRef
                        :on-click #(set-open! (not open))
                        :variant "outline"
                        :role "combobox"
                        :class-name (str "justify-between w-full")}
                (t "pool.model.entitlements.blocks.entitlements.select")

                ($ ChevronsUpDown {:class-name "ml-2 h-4 w-4 shrink-0 opacity-50"})))

          ($ PopoverContent {:class-name "p-0"
                             :style {:width (str width "px")}}
             ($ Command
                {:filter (fn [value search]
                           (let [lSearch (str/lower-case search)
                                 lValue (str/lower-case value)]
                             (if (str/includes? lValue lSearch) 1 0)))}
                ($ CommandInput {:placeholder (t "pool.model.entitlements.blocks.entitlements.select")})
                ($ CommandList
                   ($ CommandEmpty (t "pool.model.entitlements.blocks.entitlements.not_found"))
                   ($ CommandGroup
                      (for [entitlement entitlement-groups]
                        ($ CommandItem {:value (:name entitlement)
                                        :onSelect #(do (set-open! false)
                                                       (if
                                                        (not (check-path-existing (:id entitlement) fields))
                                                         (append (cj {:id nil
                                                                      :group_id (:id entitlement)
                                                                      :name (:name entitlement)
                                                                      :quantity "0"}))
                                                         (remove (find-index-from-path (:id entitlement) fields))))
                                        :key (:id entitlement)}

                           ($ Check
                              {:class-name (str "mr-2 h-4 w-4 "
                                                (if (check-path-existing (:id entitlement) fields)
                                                  "visible"
                                                  "invisible"))})
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

                         ($ TableCell {:class-name "w-[70%]"}
                            (find-name-by-id
                             entitlement-groups
                             (:group_id field)))

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
