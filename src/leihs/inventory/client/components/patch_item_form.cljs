(ns leihs.inventory.client.components.patch-item-form
  (:require
   ["@@/button" :refer [Button]]
   ["@@/form" :refer [Form]]
   ["@@/select" :refer [Select SelectContent SelectItem SelectTrigger
                        SelectValue]]
   ["@hookform/resolvers/zod" :refer [zodResolver]]
   ["date-fns" :refer [format]]
   ["lucide-react" :refer [Equal Trash]]
   ["react-hook-form" :as hook-form]
   ["react-i18next" :refer [useTranslation]]
   ["zod" :as z]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.pools.inventory.search-edit.components.field-dispatcher
    :refer [FieldDispatcher]]
   [uix.core :as uix :refer [$ defui]]))

;; Schema for a single update field entry
;; Transforms {name: "inventory_code", value: "ABC"} → {:inventory_code "ABC"}
(def update-field-schema
  (-> (z/object
       (cj {:name (z/string)
            :value (-> (z/any)
                       (.refine (fn [v] (some? v))))}))
      (.transform
       (fn [field]
         (cj {(keyword (.-name field))
              (let [val (.-value field)
                    ;; Format dates to YYYY-MM-DD if the value is a Date object
                    formatted-val (if (instance? js/Date val)
                                    (format val "yyyy-MM-dd")
                                    val)
                    ;; Coerce "true"/"false" strings to booleans
                    coerced-val (cond
                                  (= formatted-val "true") true
                                  (= formatted-val "false") false
                                  :else formatted-val)]
                (if (and (object? coerced-val) (some? (.-value coerced-val)))
                  (.-value coerced-val)
                  coerced-val))})))))

(def edit-dialog-schema
  (z/object
   (cj {:update (z/array update-field-schema)})))

;; Main EditDialog Component
(defui PatchItemForm [{:keys [blocks on-submit on-invalid]}]
  (let [[t] (useTranslation)

        form (hook-form/useForm
              #js {:resolver (zodResolver edit-dialog-schema)
                   :defaultValues (cj {:update []})})

        control (.-control form)
        {:keys [fields append remove update]} (jc (hook-form/useFieldArray
                                                   (cj {:control control
                                                        :name "update"})))

        ;; Find field names already in use
        used-field-names (set (map :name fields))

        ;; Next available block (first unused)
        next-block (->> blocks
                        (filter #(not (contains? used-field-names (:name %))))
                        first)

        ;; Build an update row entry from a block (no default value)
        create-update-entry (fn [block]
                              (-> block
                                  (assoc :id (str (random-uuid)))
                                  (dissoc :label)
                                  (assoc :value (case (:component block)
                                                  "textarea" ""
                                                  "input" ""
                                                  nil))))

        handle-add-field (fn []
                           (when next-block
                             (append (cj (create-update-entry next-block)))))

        handle-update-field (fn [field-name idx]
                              (let [selected-block (->> blocks
                                                        (filter #(= (:name %) field-name))
                                                        first)]
                                (when selected-block
                                  (update idx (cj (create-update-entry selected-block))))))

        handle-remove (fn [idx]
                        (remove idx))

        handle-submit (.. form -handleSubmit)]

    ($ Form (merge form)
       ($ :form {:id "edit-dialog-form"
                 :no-validate true
                 :on-submit (handle-submit on-submit on-invalid)}

          ($ :div {:class-name "space-y-2 py-2"}
              ;; Render all update field rows
             (for [[idx field] (map-indexed vector fields)]
               ($ :div {:key (:id field)
                        :class-name "grid grid-cols-12 items-center gap-2"}

                  ;; Field selector
                  ($ Select {:value (:name field)
                             :onValueChange #(handle-update-field % idx)}
                     ($ SelectTrigger {:data-test-id (str "field-select-" idx)
                                       :class-name "col-span-4"}
                        ($ SelectValue))
                     ($ SelectContent {:data-test-id "field-options"}
                        (for [block blocks]
                          ($ SelectItem {:key (:name block)
                                         :disabled (and (not= (:name block) (:name field))
                                                        (contains? used-field-names (:name block)))
                                         :value (:name block)}

                             ($ :button {:type "button"}
                                (t (:label block)))))))

                  ($ Equal {:class-name "col-span-1 justify-self-center"})

                  ;; Value input (no default value)
                  ($ :div {:class-name "col-span-6"}
                     ($ FieldDispatcher {:form form
                                         :block (assoc field
                                                       :name (str "update." idx ".value"))}))

                  ;; Remove button
                  ($ Button {:type "button"
                             :variant "outline"
                             :size "icon"
                             :on-click #(handle-remove idx)
                             :class-name "col-span-1 self-center justify-self-end"}
                     ($ Trash {:class-name "h-4 w-4"}))))

             ;; Add field button
             ($ Button {:type "button"
                        :variant "outline"
                        :size "sm"
                        :on-click handle-add-field
                        :disabled (nil? next-block)}
                (t "pool.models.search_edit.dialog.add_field")))))))
