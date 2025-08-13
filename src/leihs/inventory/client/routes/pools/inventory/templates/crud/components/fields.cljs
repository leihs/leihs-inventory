(ns leihs.inventory.client.routes.pools.inventory.templates.crud.components.fields
  (:require
   ["@@/checkbox" :refer [Checkbox]]
   ["@@/dropzone" :refer [Dropzone]]
   ["@@/form" :refer [FormField FormItem FormLabel FormControl FormDescription FormMessage]]
   ["@@/input" :refer [Input]]
   ["@@/textarea" :refer [Textarea]]
   ["react-i18next" :refer [useTranslation]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.pools.inventory.templates.crud.components.entitlements :refer [Entitlements]]
   [uix.core :as uix :refer [defui $]]))

(def fields-map
  {"input" Input
   "dropzone" Dropzone
   "textarea" Textarea})

(defui field [{:keys [control form block]}]
  (let [[t] (useTranslation)]
    (case (:component block)
      "entitlements"
      ($ Entitlements {:form form
                       :name (:name block)
                       :label (:label block)
                       :props (:props block)})

      "checkbox"
      ($ FormField {:control (cj control)
                    :name (:name block)
                    :render #($ FormItem {:class-name "mt-6"}
                                ($ FormControl
                                   ($ Checkbox (merge
                                                {:checked (-> (jc %) :field :value)
                                                 :onCheckedChange (-> (jc %) :field :onChange)}
                                                (:props block))))

                                ($ FormLabel {:className "pl-4"} (t (:label block)))
                                ($ FormMessage))})

      ;; "default case - this renders a component from the component map"
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
                                       ($ :<> (:description block)))

                                    ($ FormMessage))}))))))
