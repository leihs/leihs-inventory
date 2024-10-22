(ns leihs.inventory.client.routes.models.create.fields
  (:require
   ["@@/checkbox" :refer [Checkbox]]
   ["@@/dropzone" :refer [Dropzone]]
   ["@@/form" :refer [FormField FormItem FormLabel FormControl FormDescription FormMessage]]
   ["@@/input" :refer [Input]]
   ["@@/textarea" :refer [Textarea]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.models.create.components.accessories-list :refer [AccessoryList]]
   [leihs.inventory.client.routes.models.create.components.model-properties :refer [ModelProperties]]
   [uix.core :as uix :refer [defui $]]))

(def fields-map
  {"input" Input
   "dropzone" Dropzone
   "textarea" Textarea
   "checkbox" Checkbox})

(defui field [{:keys [control block]}]
  (cond
    (-> block :component (= "accessory-list"))
    ($ AccessoryList {:control control
                      :props (:props block)})

    (-> block :component (= "model-properties"))
    ($ ModelProperties {:control control
                        :props (:props block)})

    (-> block :input (= "checkbox"))
    ($ FormField {:control (cj control)
                  :name (:name block)
                  :render #($ FormItem {:class-name "mt-6"}
                              ($ FormControl
                                 ($ Checkbox (merge
                                              {:checked (-> (jc %) :field :value)
                                               :onCheckedChange (-> (jc %) :field :onChange)}
                                              (:props block))))

                              ($ FormLabel {:className "pl-4"} (:label block))
                              ($ FormDescription
                                 ($ :<> (:description block)))

                              ($ FormMessage))})

    comment "default case - this renders a component from the component map"
    :else
    (let [comp (get fields-map (:input block))]
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
