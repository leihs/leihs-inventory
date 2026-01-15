(ns leihs.inventory.client.routes.pools.inventory.entitlement-groups.crud.components.fields
  (:require
   ["@@/form" :refer [FormField FormItem FormLabel FormControl FormDescription FormMessage]]
   ["@@/input" :refer [Input]]
   ["@@/radio-group" :refer [RadioGroup RadioGroupItem]]
   ["@@/table" :refer [TableCell]]
   ["@@/tooltip" :refer [Tooltip TooltipTrigger TooltipContent]]
   ["react-i18next" :refer [useTranslation]]
   [leihs.inventory.client.components.form.groups :refer [Groups]]
   [leihs.inventory.client.components.form.models :refer [Models]]
   [leihs.inventory.client.components.form.users :refer [Users]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [defui $]]))

(defui field [{:keys [control form block]}]
  (let [[t] (useTranslation)]
    (case (:component block)
      "radio-group"
      ($ FormField {:control (cj control)
                    :name (:name block)
                    :render #($ FormItem {:class-name "mt-6"
                                          :title (when (:disabled (:props block))
                                                   "This field is disabled/protected.")}
                                ($ FormLabel (t (:label block))
                                   (when (-> block :props :required) "*"))

                                ($ FormControl
                                   ($ RadioGroup {:onValueChange (aget % "field" "onChange")
                                                  :defaultValue (aget % "field" "value")
                                                  :class-name "flex space-x-1"
                                                  :name (:name block)}

                                      (for [option (:options (:props block))]
                                        ($ FormItem {:key (:value option)
                                                     :class-name "flex items-center space-x-2 space-y-0"}
                                           ($ FormControl
                                              ($ RadioGroupItem {:data-test-id (str (:name block) "-" (:value option))
                                                                 :disabled (:disabled (:props block))
                                                                 :value (:value option)}))
                                           ($ FormLabel {:class-name "font-normal"}
                                              (t (:label option))))))))})

      "models"
      ($ Models {:form form
                 :name (:name block)
                 :label (:label block)
                 :required (:required block)
                 :props (:props block)}
         (fn [update index field]
           ;; cells to be inserted into the generic model row component
           (let [available (:available field)
                 entitled-in-groups (or (:entitled_in_other_groups field) (:entitled_in_groups field))
                 net-available (- available entitled-in-groups)
                 quantity (:quantity field)
                 quantity-exceeds-availability? (and quantity (> quantity net-available))
                 quantity-too-low? (and quantity (< quantity 1))]
             ($ :<>
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

                ($ TableCell {:class-name "w-[5rem]"}
                   ($ Input {:class-name "text-right"
                             :type "number"
                             :data-test-id "quantity"
                             :value (if (:quantity field)
                                      (:quantity field)
                                      1)
                             :onChange (fn [event]
                                         (update
                                          index
                                          (cj (merge field
                                                     {:quantity (.. event -target -value)}))))}))
                ($ TableCell {:class-name "px-0"} "/")
                ($ TableCell
                   ($ Tooltip
                      ($ TooltipTrigger {:asChild true}
                         ($ :span {:data-test-id "entitled_in_other_groups"} net-available))
                      ($ TooltipContent {:className "max-w-[20rem]"}
                         (t "pool.entitlement_groups.entitlement_group.models.blocks.models.available_count_tooltip"))))
                ($ TableCell {:class-name "px-0"} "/")
                ($ TableCell
                   ($ Tooltip
                      ($ TooltipTrigger {:asChild true}
                         ($ :span {:data-test-id "available"} available))
                      ($ TooltipContent {:className "max-w-[20rem]"}
                         (t "pool.entitlement_groups.entitlement_group.models.blocks.models.items_count_tooltip"))))))))

      "users"
      ($ Users {:form form
                :name (:name block)
                :label (:label block)
                :required (:required block)
                :props (:props block)})

      "groups"
      ($ Groups {:form form
                 :name (:name block)
                 :label (:label block)
                 :required (:required block)
                 :props (:props block)})

      ;; default case - render a plain component according to the map
      (let [field-map {"input" Input}
            comp (get field-map (:component block))]
        (if comp
          ($ FormField {:control (cj control)
                        :name (:name block)
                        :render #($ FormItem {:class-name "mt-6"}

                                    ($ FormLabel (t (:label block)) (when (:required (:props block)) "*"))
                                    ($ FormControl
                                       ($ comp (merge
                                                (dissoc (:props block) :required)
                                                (:field (jc %)))))

                                    ($ FormDescription
                                       ($ :<> (:description block)))

                                    ($ FormMessage))})
          ($ :div "Unsupported field component '" (:component block) "'"))))))
