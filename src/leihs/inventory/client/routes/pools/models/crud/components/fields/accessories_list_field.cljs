(ns leihs.inventory.client.routes.pools.models.crud.components.fields.accessories-list-field
  (:require
   ["@@/button" :refer [Button]]
   ["@@/form" :refer [FormControl FormField FormItem FormDescription FormMessage]]
   ["@@/input" :refer [Input]]
   ["@@/table" :refer [TableCell]]
   ["lucide-react" :refer [CirclePlus]]
   ["react-i18next" :refer [useTranslation]]
   [leihs.inventory.client.components.form.form-field-array :refer [FormFieldArray FormFieldArrayItems
                                                                    use-array-items use-array-item]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom :as uix-dom]))

;; AddAccessory - Button for adding new accessories
(defui AddAccessory []
  (let [[t] (useTranslation)
        {:keys [fields append]} (use-array-items)
        handle-add (fn []
                     (uix-dom/flush-sync
                      (append #js {:name ""})) ; Omit id to avoid null warning
                     (let [selector (str "input[name='accessories." (count fields) ".name']")
                           next (js/document.querySelector selector)]
                       (when next (.focus next))))]
    ($ Button {:type "button"
               :variant "outline"
               :on-click handle-add}
       ($ CirclePlus {:className "w-4 h-4"})
       (t "pool.model.accessories.blocks.accessories.add_button"))))

;; AccessoryItem - Table cell for one accessory row
(defui AccessoryItem []
  (let [{:keys [form name index]} (use-array-item)
        {:keys [fields append]} (use-array-items) ; For Enter key handler
        control (.-control form)

        add-field (fn []
                    (uix-dom/flush-sync
                     (append #js {:name ""}))
                    (let [selector (str "input[name='accessories." (count fields) ".name']")
                          next (js/document.querySelector selector)]
                      (when next (.focus next))))

        handle-enter (fn [ev]
                       (when (= (.. ev -key) "Enter")
                         (.. ev (preventDefault))
                         (add-field)))]

    ($ :<>
       ($ TableCell
          ($ FormField
             {:control (cj control)
              :name (str name "." index ".name")
              :render #($ FormItem
                          ($ FormControl
                             ($ Input (merge
                                       {:data-index index
                                        :on-key-down handle-enter}
                                       (:field (jc %)))))
                          ($ FormMessage))})))))

(defui AccessoryListField [{:keys [form block]}]
  ($ FormField {:control (.-control form)
                :name (:name block)
                :render #($ FormFieldArray {:form form
                                            :name (:name block)}

                            ($ FormFieldArrayItems {:form form
                                                    :name (:name block)}
                               ($ AccessoryItem))

                            ($ FormItem
                               ($ FormControl
                                  ($ AddAccessory))

                               ($ FormDescription
                                  ($ :<> (:description block)))

                               ($ FormMessage)))}))
