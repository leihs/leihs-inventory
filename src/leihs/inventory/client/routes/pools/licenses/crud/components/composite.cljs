(ns leihs.inventory.client.routes.pools.licenses.crud.components.composite
  (:require
   ["@@/button" :refer [Button]]
   ["@@/form" :refer [FormControl FormField FormDescription FormItem FormLabel FormMessage]]
   ["@@/input" :refer [Input]]
   ["@@/table" :refer [TableCell]]
   ["lucide-react" :refer [Check ChevronDown CirclePlus]]

   ["react-i18next" :refer [useTranslation]]
   [leihs.inventory.client.components.form.form-field-array :refer [FormFieldArray
                                                                    FormFieldArrayItems
                                                                    use-array-items
                                                                    use-array-item]]
   [leihs.inventory.client.components.typo :refer [Typo]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [$ defui]]))

(defui AddButton [{:keys [className]}]
  (let [{:keys [append]} (use-array-items)
        [t] (useTranslation)
        handle-click (fn [] (append #js {:quantity 0
                                         :location ""}))]
    ($ FormControl
       ($ Button {:type "button"
                  :variant "outline"
                  :on-click handle-click
                  :class-name (str "flex " className)}
          ($ CirclePlus {:class-name "h-4 w-4"})
          (t "pool.licenses.license.fields.entitlements.add")))))

(defui CompositeItem []
  (let [{:keys [form name index]} (use-array-item)
        [t] (useTranslation)
        control (aget form "control")]
    ($ :<>
       ($ TableCell {:class-name ""}
          ($ FormField {:control control
                        :name (str name "." index ".quantity")
                        :render #($ FormItem {:class-name "flex items-center space-x-2 space-y-0"}
                                    ($ FormLabel (t "pool.licenses.license.fields.entitlements.quantity"))
                                    ($ FormControl
                                       ($ Input (merge (:field (jc %))
                                                       {:type "number"
                                                        :min 0
                                                        :class-name "w-full"})))

                                    ($ FormMessage))}))

        ;; Name cell
       ($ TableCell {:class-name ""}
          ($ FormField {:control control
                        :name (str name "." index ".location")
                        :render #($ FormItem {:class-name "flex items-center space-x-2 space-y-0"}
                                    ($ FormLabel (t "pool.licenses.license.fields.entitlements.location"))
                                    ($ FormControl
                                       ($ Input (merge (:field (jc %))
                                                       {:type "text"
                                                        :class-name "w-full"})))

                                    ($ FormMessage))})))))

(defui Composite [{:keys [form block]}]
  (let [[t] (useTranslation)
        get-values (aget form "getValues")
        total-quantitiy (int (get-values "properties_total_quantity"))
        total-allocations (reduce + (map #(int (:quantity %))
                                         (jc (get-values (:name block)))))
        remaining (- total-quantitiy total-allocations)]

    ($ FormFieldArray {:form form
                       :name (:name block)}
       ($ FormItem {:class-name "mt-6 flex flex-col"}
          ($ :div {:class-name "flex items-center"}
             ($ FormLabel (t (:label block)) (when (:required (:props block)) "*"))
             ($ Typo {:variant "label"
                      :class-name (str "ml-auto " (when (< remaining 0) "text-red-500"))}
                (str (t "pool.licenses.license.fields.entitlements.remaining") " " remaining)))

          ($ FormDescription
             ($ :<> (:description block)))

          ($ FormMessage)

          ($ FormFieldArrayItems {:form form
                                  :name (:name block)}
             ($ CompositeItem))

          ($ AddButton {:className "w-fit"})))))
