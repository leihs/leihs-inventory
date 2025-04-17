(ns leihs.inventory.client.routes.items.crud.components.fields
  (:require
   ["@@/checkbox" :refer [Checkbox]]
   ["@@/dropzone" :refer [Dropzone]]
   ["@@/form" :refer [FormField FormItem FormLabel FormControl FormDescription FormMessage]]
   ["@@/input" :refer [Input]]
   ["@@/textarea" :refer [Textarea]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [defui $]]))

(def fields-map
  {"input" Input
   "dropzone" Dropzone
   "textarea" Textarea})

(defui field [{:keys [control form block]}]
  (cond
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
