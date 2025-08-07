(ns leihs.inventory.client.routes.pools.options.crud.components.fields
  (:require
   ["@@/dropzone" :refer [Dropzone]]
   ["@@/form" :refer [FormField FormItem FormLabel FormControl FormDescription FormMessage]]
   ["@@/input" :refer [Input]]
   ["@@/textarea" :refer [Textarea]]
   ["react-i18next" :refer [useTranslation]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [defui $]]))

(def fields-map
  {"input" Input
   "dropzone" Dropzone
   "textarea" Textarea})

(defui field [{:keys [control form block]}]
  (let [[t] (useTranslation)]
    (cond
      ;; "default case - this renders a component from the component map"
      :else
      (let [comp (get fields-map (:component block))]
        (when comp
          ($ FormField {:control (cj control)
                        :name (:name block)
                        :render #($ FormItem {:class-name "mt-6"}

                                    ($ FormLabel (t (:label block)))
                                    ($ FormControl
                                       ($ comp (merge
                                                (:props block)
                                                (:field (jc %)))))

                                    ($ FormDescription
                                       ($ :<> (t (:description block))))

                                    ($ FormMessage))}))))))
