(ns leihs.inventory.client.routes.pools.inventory.entitlement-groups.crud.components.fields
  (:require
   ["@@/form" :refer [FormField FormItem FormLabel FormControl FormDescription FormMessage]]
   ["@@/input" :refer [Input]]
   ["@@/radio-group" :refer [RadioGroup RadioGroupItem]]
   ["react-i18next" :refer [useTranslation]]
   [leihs.inventory.client.components.form.form-field-array :refer [FormFieldArray FormFieldArrayItems]]
   [leihs.inventory.client.components.form.select-model :refer [SelectModel]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.pools.inventory.entitlement-groups.crud.components.group-item :refer [GroupItem]]
   [leihs.inventory.client.routes.pools.inventory.entitlement-groups.crud.components.model-item :refer [ModelItem]]
   [leihs.inventory.client.routes.pools.inventory.entitlement-groups.crud.components.select-group :refer [SelectGroup]]
   [leihs.inventory.client.routes.pools.inventory.entitlement-groups.crud.components.select-user :refer [SelectUser]]
   [leihs.inventory.client.routes.pools.inventory.entitlement-groups.crud.components.user-item :refer [UserItem]]
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
            ($ ModelItem)))

      "users"
      ($ FormFieldArray {:form form
                         :name (:name block)}
         ($ FormItem {:class-name "mt-6"}
            ($ FormLabel (t (:label block)) (when (:required (:props block)) "*"))
            ($ SelectUser {:form form
                           :name (:name block)
                           :props (:props block)})

            ($ FormDescription
               ($ :<> (:description block)))

            ($ FormMessage))

         ($ FormFieldArrayItems {:form form
                                 :name (:name block)}
            ($ UserItem)))

      "groups"
      ($ FormFieldArray {:form form
                         :name (:name block)}
         ($ FormItem {:class-name "mt-6"}
            ($ FormLabel (t (:label block)) (when (:required (:props block)) "*"))
            ($ SelectGroup {:form form
                            :name (:name block)
                            :props (:props block)})

            ($ FormDescription
               ($ :<> (:description block)))

            ($ FormMessage))

         ($ FormFieldArrayItems {:form form
                                 :name (:name block)}
            ($ GroupItem)))

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
