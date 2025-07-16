(ns leihs.inventory.client.components.basic-form-field
  (:require
   ["@@/checkbox" :refer [Checkbox]]
   ["@@/dropzone" :refer [Dropzone]]
   ["@@/form" :refer [FormField FormItem FormLabel FormControl FormDescription FormMessage]]
   ["@@/input" :refer [Input]]
   ["@@/textarea" :refer [Textarea]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.models.create.components.accessories-list :refer [AccessoryList]]
   [uix.core :as uix :refer [defui $]]))

;; "available form fields"
(def fields-map
  {"input" Input
   "dropzone" Dropzone
   "textarea" Textarea
   "checkbox" Checkbox
   "accessory-list" AccessoryList})

(defui main
  "Main function for rendering a form field component.

  Arguments:
  - `control`: The control element for the form field.
  - `input`: A map containing input properties.
  - `class-name`: A string for additional CSS class names for the input field
  - `label`: A boolean indicating whether to display the label.
  - `description`: A boolean indicating whether to display the description.
  - `name`: The name of the form field.

  Default values:
  - `label`: true
  - `description`: true
  - `class-name`: empty string
  - `name`: The name from the `input` map."

  [{:keys [control input class-name
           label description name]
    :or {label true description true
         class-name "" name (:name input)}}]

  (let [comp (get fields-map (:component input))]
    (when comp
      ($ FormField {:control (cj control)
                    :name name
                    :render #($ FormItem
                                (when label ($ FormLabel (:label input)))
                                ($ FormControl
                                   ($ comp (merge
                                            {:class-name class-name}
                                            (:props input)
                                            (:field (jc %)))))

                                (when description ($ FormDescription
                                                     ($ :<> (:description input))))

                                ($ FormMessage))}))))
