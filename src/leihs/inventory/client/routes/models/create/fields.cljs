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

(defui fields [{:keys [block field]}]
  ($ FormItem {:class-name "mt-6"}
     (let [input (:input block)]
       (cond
         (-> input :component (= "input"))
         ($ FormLabel (:label block)
            ($ FormControl
               ($ Input (merge
                         (:props input)
                         (:field (jc field))))))

         (-> input :component (= "dropzone"))
         ($ FormLabel (:label block)
            ($ FormControl
               ($ Dropzone (merge
                            (:props input)
                            (:field (jc field))))))

         (-> input :component (= "accessory-list"))
         ($ FormLabel (:label block)
            ($ FormControl
               ($ AccessoryList (merge
                                 (:props input)
                                 (:field (jc field))))))

         (-> input :component (= "model-properties"))
         ($ FormLabel (:label block)
            ($ FormControl
               ($ ModelProperties (merge
                                   (:props input)
                                   (:field (jc field))))))

         (-> input :component (= "textarea"))
         ($ FormLabel (:label block)
            ($ FormControl
               ($ Textarea (merge
                            (:props input)
                            (:field (jc field))))))

         (-> input :component (= "checkbox"))
         ($ :<>
            ($ FormControl
               ($ Checkbox (merge
                            {:checked (-> (jc field) :field :value)
                             :onCheckedChange (-> (jc field) :field :onChange)}
                            (:props input))))

            ($ FormLabel {:className "pl-4"} (:label block)))

         :else
         ($ :div "input type not implemented -> " (:component input))))

     ($ FormDescription
        ($ :<> (:description block)))

     ($ FormMessage)))

(defui field [{:keys [control block]}]
  ($ FormField {:control (cj control)
                :name (:name block)
                :render (fn [field]
                          ($ fields {:block block
                                     :field field}))}))
