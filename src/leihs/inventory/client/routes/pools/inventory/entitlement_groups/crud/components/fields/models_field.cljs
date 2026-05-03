(ns leihs.inventory.client.routes.pools.inventory.entitlement-groups.crud.components.fields.models-field
  (:require
   ["@@/form" :refer [FormControl FormField FormItem FormLabel FormDescription FormMessage]]
   ["@@/input" :refer [Input]]
   ["@@/table" :refer [TableCell]]
   ["@@/tooltip" :refer [Tooltip TooltipContent TooltipTrigger]]
   ["react-i18next" :refer [useTranslation]]
   [leihs.inventory.client.components.form.form-field-array :refer [FormFieldArray FormFieldArrayItems use-array-item]]
   [leihs.inventory.client.components.form.select-model :refer [SelectModel]]
   [leihs.inventory.client.components.image-cell :refer [ImageCell]]
   [leihs.inventory.client.lib.utils :refer [jc]]
   [uix.core :as uix :refer [$ defui]]))

(defui ModelItem []
  (let [[t] (useTranslation)
        {:keys [field index form]} (use-array-item)
        available (:borrowable_quantity field)
        entitled-in-groups (or (:entitled_in_other_groups field) (:entitled_in_groups field))
        net-available (- available entitled-in-groups)
        quantity (:quantity field)
        quantity-exceeds-availability? (and quantity (> quantity net-available))
        quantity-too-low? (and quantity (< quantity 1))]
    ($ :<>
       ;; Image cell with preview dialog
       ($ ImageCell {:field field})

       ;; Name cell
       ($ TableCell {:class-name ""}
          (str (:name field)))

       ;; Error/warning cell
       ($ TableCell {:class-name "w-1/5"}
          (cond
            quantity-exceeds-availability?
            ($ :span {:class-name "text-red-500"}
               (t "pool.entitlement_groups.entitlement_group.quantity_error"))

            quantity-too-low?
            (let [models-err (aget (aget form "formState" "errors") "models")]
              (when (and models-err (aget models-err index))
                ($ :span {:class-name "text-red-500"}
                   (aget models-err index "quantity" "message"))))))

       ;; Quantity input cell
       ($ TableCell {:class-name "w-[5rem]"}
          ($ FormField
             {:control (.-control form)
              :name (str "models." index ".quantity")
              :render #($ FormItem
                          ($ FormControl
                             ($ Input (merge
                                       {:class-name "text-right"
                                        :type "number"
                                        :data-test-id "quantity"}
                                       (:field (jc %))))))}))

       ;; Separator
       ($ TableCell {:class-name "px-0"} "/")

       ;; Net available (with tooltip)
       ($ TableCell
          ($ Tooltip
             ($ TooltipTrigger {:asChild true}
                ($ :span {:data-test-id "entitled_in_other_groups"} net-available))
             ($ TooltipContent {:className "max-w-[20rem]"}
                (t "pool.entitlement_groups.entitlement_group.models.blocks.models.available_count_tooltip"))))

       ;; Separator
       ($ TableCell {:class-name "px-0"} "/")

       ;; Total available (with tooltip)
       ($ TableCell
          ($ Tooltip
             ($ TooltipTrigger {:asChild true}
                ($ :span {:data-test-id "available"} available))
             ($ TooltipContent {:className "max-w-[20rem]"}
                (t "pool.entitlement_groups.entitlement_group.models.blocks.models.items_count_tooltip")))))))

(defui ModelsField [{:keys [form block]}]
  (let [[t] (useTranslation)]
    ($ FormFieldArray {:form form
                       :name (:name block)}
       ($ FormItem {:class-name "mt-6"}
          ($ FormLabel (t (:label block)) (when (:required (:props block)) "*"))
          ($ SelectModel {:form form
                          :name (:name block)
                          :props (:props block)})

          ($ FormDescription
             ($ :<> (:description block)))

          ($ FormMessage))

       ($ FormFieldArrayItems {:form form
                               :name (:name block)}
          ($ ModelItem)))))
