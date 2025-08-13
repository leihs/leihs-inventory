(ns leihs.inventory.client.routes.pools.inventory.templates.crud.components.fields
  (:require
   ["@@/dropzone" :refer [Dropzone]]
   ["@@/form" :refer [FormField FormItem FormLabel FormControl FormDescription FormMessage]]
   ["@@/input" :refer [Input]]
   ["@@/table" :refer [TableCell]]
   ["@@/textarea" :refer [Textarea]]
   ["react-i18next" :refer [useTranslation]]
   [leihs.inventory.client.components.form.models :refer [Models]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [defui $]]))

(def fields-map
  {"input" Input
   "dropzone" Dropzone
   "textarea" Textarea})

(defui field [{:keys [control form block]}]
  (let [[t] (useTranslation)]
    (case (:component block)
      "models"
      ($ Models {:form form
                 :name (:name block)
                 :label (:label block)
                 :required (:required block)
                 :props (:props block)}
         (fn [update index field]
           ($ :<>
              ($ TableCell {:class-name "w-1/5"}

                 (cond
                   (and (:quantity field)
                        (> (:quantity field)
                           (:available field)))
                   ($ :span {:class-name "text-red-500"}
                      (t "pool.templates.template.quantity_error"))

                   (and (:quantity field)
                        (< (:quantity field) 0))
                   (let [models-err (aget (aget form "formState" "errors") "models")]
                     (when (and models-err (aget models-err index))
                       ($ :span {:class-name "text-red-500"}
                          (aget models-err index "quantity" "message"))))))

              ($ TableCell {:class-name "w-[5rem]"}
                 ($ Input {:type "number"
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

              ($ TableCell (:available field)))))

      ;; "default case - this renders a component from the component map"
      (let [comp (get fields-map (:component block))]
        (when comp
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

                                    ($ FormMessage))}))))))
