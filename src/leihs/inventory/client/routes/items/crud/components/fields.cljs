(ns leihs.inventory.client.routes.items.crud.components.fields
  (:require
   ["@@/checkbox" :refer [Checkbox]]
   ["@@/dropzone" :refer [Dropzone]]
   ["@@/form" :refer [FormField FormItem FormLabel FormControl FormDescription FormMessage]]
   ["@@/input" :refer [Input]]
   ["@@/radio-group" :refer [RadioGroup RadioGroupItem]]
   ["@@/select" :refer [Select SelectTrigger SelectValue
                        SelectContent SelectItem]]
   ["@@/textarea" :refer [Textarea]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.items.crud.components.inventory-code :refer [InventoryCode]]
   [uix.core :as uix :refer [defui $]]))

(def fields-map
  {"input" Input
   "dropzone" Dropzone
   "textarea" Textarea})

(defui field [{:keys [control form block]}]
  (cond
    (-> block :component (= "inventory-code"))
    ($ InventoryCode {:control control
                      :props (:props block)})

    (-> block :component (= "checkbox"))
    ($ FormField {:control (cj control)
                  :name (:name block)
                  :render #($ FormItem {:class-name "mt-6"}
                              ($ FormControl
                                 ($ Checkbox (merge
                                              {:checked (-> (jc %) :field :value)
                                               :onCheckedChange (-> (jc %) :field :onChange)}
                                              (:props block))))

                              ($ FormLabel {:className "pl-4"} (:label block))
                              ($ FormMessage))})

    (-> block :component (= "radio-group"))
    ($ FormField {:control (cj control)
                  :name (:name block)
                  :render #($ FormItem {:class-name "mt-6"}
                              ($ FormLabel (:label block))

                              ($ FormControl
                                 ($ RadioGroup {:onValueChange (aget % "field" "onChange")
                                                :defaultValue (aget % "field" "value")
                                                :class-name "flex space-x-1"}

                                    (js/console.debug %)
                                    (for [option (:options (:props block))]
                                      ($ FormItem {:key (:value option)
                                                   :class-name "flex items-center space-x-2 space-y-0"}
                                         ($ FormControl
                                            ($ RadioGroupItem {:value (:value option)}))
                                         ($ FormLabel {:class-name "font-normal"}
                                            (:label option)))))))})

    (-> block :component (= "select"))
    ($ FormField {:control (cj control)
                  :name (:name block)
                  :render #($ FormItem {:class-name "mt-6"}
                              ($ FormLabel (:label block))

                              ($ Select {:onValueChange (aget % "field" "onChange")
                                         :defaultValue (aget % "field" "value")}

                                 ($ FormControl
                                    ($ SelectTrigger
                                       ($ SelectValue {:placeholder (:placeholder (:props block))})))

                                 ($ SelectContent
                                    (for [option (:options (:props block))]
                                      ($ SelectItem {:key (:value option)
                                                     :value (:value option)}
                                         ($ :<> (:label option)))))
                                 ($ FormMessage)))})

    ;; "default case - this renders a component from the component map"
    :else
    (let [comp (get fields-map (:component block))]
      (when comp
        ($ FormField {:control (cj control)
                      :name (:name block)
                      :render #($ FormItem {:class-name "mt-6"}
                                  ($ FormLabel (:label block))
                                  ($ FormControl
                                     ($ comp (merge
                                              (:props block)
                                              (:field (jc %)))))

                                  ($ FormDescription
                                     ($ :<> (:description block)))

                                  ($ FormMessage))})))))
