(ns leihs.inventory.client.routes.pools.models.crud.components.fields.entitlement-allocations-field
  (:require
   ["@/components/ui/command" :refer [Command CommandEmpty CommandGroup
                                      CommandInput CommandItem CommandList]]
   ["@/components/ui/popover" :refer [Popover PopoverContent PopoverTrigger]]
   ["@@/button" :refer [Button]]
   ["@@/form" :refer [FormControl FormField FormItem FormMessage]]
   ["@@/input" :refer [Input]]
   ["@@/label" :refer [Label]]
   ["@@/table" :refer [TableCell TableHead TableHeader TableRow]]
   ["lucide-react" :refer [Check ChevronsUpDown]]
   ["react-hook-form" :refer [useWatch]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :refer [useLoaderData]]
   [leihs.inventory.client.components.form.form-field-array :refer [FormFieldArray
                                                                    FormFieldArrayItems
                                                                    use-array-item
                                                                    use-array-items]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [$ defui]]))

(defn find-name-by-id [vec id]
  (some #(when (= (:id %) id) (:name %)) vec))

(defn check-path-existing [entitlement items]
  (some (fn [item]
          (= entitlement (:group_id item)))
        items))

(defn find-index-from-path [path items]
  (some (fn [[idx item]]
          (when (= path (:group_id item))
            idx))
        (map-indexed vector items)))

;; Table header component
(defui EntitlementTableHeader []
  (let [[t] (useTranslation)]
    ($ TableHeader
       ($ TableRow
          ($ TableHead {:class-name "w-4 p-0"} "")
          ($ TableHead (t "pool.model.entitlements.blocks.entitlements.label_group"))
          ($ TableHead (t "pool.model.entitlements.blocks.entitlements.label_quantity"))))))

;; Combobox for selecting/deselecting entitlement groups
(defui EntitlementGroupSelector []
  (let [{:keys [entitlement-groups data]} (jc (useLoaderData))
        {:keys [fields append remove]} (use-array-items)
        [t] (useTranslation)
        rentable (or (:rentable data) 0)
        [open set-open!] (uix/use-state false)
        [width set-width!] (uix/use-state nil)
        button-ref (uix/use-ref nil)]

    (uix/use-effect
     (fn []
       (when (.. button-ref -current)
         (set-width! (.. button-ref -current -offsetWidth))))
     [])

    ($ :div {:class-name "flex flex-col gap-2"}
       ($ Label (t "pool.model.entitlements.blocks.entitlements.label"
                   #js {:amount rentable}))

       ($ Popover {:open open
                   :on-open-change #(set-open! %)}
          ($ PopoverTrigger {:as-child true}
             ($ Button {:ref button-ref
                        :on-click #(set-open! (not open))
                        :variant "outline"
                        :role "combobox"
                        :type "button"
                        :class-name "justify-between w-full"}
                (t "pool.model.entitlements.blocks.entitlements.select")
                ($ ChevronsUpDown {:class-name "ml-2 h-4 w-4 shrink-0 opacity-50"})))

          ($ PopoverContent {:class-name "p-0"
                             :style {:width (str width "px")}}
             ($ Command
                ($ CommandInput {:placeholder (t "pool.model.entitlements.blocks.entitlements.select")})
                ($ CommandList
                   ($ CommandEmpty (t "pool.model.entitlements.blocks.entitlements.not_found"))
                   ($ CommandGroup
                      (for [entitlement entitlement-groups]
                        ($ CommandItem
                           {:value (:id entitlement)
                            :keywords #js [(:name entitlement)]
                            :onSelect #(do
                                         (set-open! false)
                                         (if (not (check-path-existing (:id entitlement) fields))
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

                           ($ :button {:type "button"}
                              (:name entitlement))))))))))))

;; Row item component — rendered per field by FormFieldArrayItems
(defui EntitlementAllocationItem []
  (let [{:keys [form name index field]} (use-array-item)
        {:keys [entitlement-groups data]} (jc (useLoaderData))
        control (.-control form)
        fields (jc (useWatch #js {:control control
                                  :name name}))
        rentable (or (:rentable data) 0)

        allocations (reduce (fn [acc item]
                              (+ acc (js/parseInt (or (:quantity item) "0"))))
                            0
                            fields)]

    ($ :<>
       ;; Color indicator cell
       ($ TableCell {:class-name (str "w-4 h-full p-0 "
                                      (if (> (+ (int rentable) 1)
                                             (int allocations))
                                        "bg-green-500"
                                        "bg-red-500"))})

       ;; Group name cell
       ($ TableCell {:class-name "w-[70%]"}
          (find-name-by-id entitlement-groups (:group_id field)))

       ;; Quantity field
       ($ TableCell
          ($ FormField
             {:control (cj control)
              :name (str name "." index ".quantity")
              :render #($ FormItem
                          ($ FormControl
                             ($ Input (merge
                                       {:className ""
                                        :type "number"
                                        :min "0"}
                                       (:field (jc %)))))
                          ($ FormMessage))})))))

;; Top-level field component — mirrors ModelProperties structure
(defui EntitlementAllocationsField [{:keys [block form]}]
  ($ FormField
     {:control (.-control form)
      :name (:name block)
      :render #($ FormFieldArray {:form form
                                  :name (:name block)}

                  ($ FormItem
                     ($ FormControl
                        ($ EntitlementGroupSelector))
                     ($ FormMessage))

                  ($ FormFieldArrayItems {:form form
                                          :name (:name block)}

                     ($ EntitlementAllocationItem)))}))
