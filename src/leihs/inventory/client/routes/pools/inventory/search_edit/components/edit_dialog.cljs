(ns leihs.inventory.client.routes.pools.inventory.search-edit.components.edit-dialog
  (:require
   ["@@/button" :refer [Button]]
   ["@@/dialog" :refer [Dialog DialogContent DialogDescription DialogFooter
                        DialogHeader DialogTitle]]
   ["@@/form" :refer [Form]]
   ["@@/select" :refer [Select SelectContent SelectItem SelectTrigger
                        SelectValue]]
   ["@hookform/resolvers/zod" :refer [zodResolver]]

   ["lucide-react" :refer [ChevronLeft ChevronRight Equal Trash]]
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
            :value (z/any)}))
      (.transform
       (fn [field]
         (cj {(keyword (.-name field))
              (let [val (.-value field)]
                (if (and (object? val) (some? (.-value val)))
                  (.-value val)
                  val))})))))

(def edit-dialog-schema
  (z/object
   (cj {:update (z/array update-field-schema)})))

;; Main EditDialog Component
(defui EditDialog [{:keys [open? on-open-change selected-items blocks]}]
  (let [item-count (count selected-items)

        [t] (useTranslation)

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

        handle-submit (.. form -handleSubmit)

        on-submit (fn [data]
                    (let [update-data (jc (.-update data))]
                      (js/console.log "Edit submitted:"
                                      (clj->js {:selected-items (vec selected-items)
                                                :update update-data}))
                      (on-open-change false)))

        on-invalid (fn [errors]
                     (js/console.warn "Edit dialog validation failed:" (clj->js errors)))]

    ;; Reset the form whenever the dialog opens
    (uix/use-effect
     (fn []
       (when open?
         (.reset form (cj {:update []}))))
     [open? form])

    ($ Dialog {:open open?
               :onOpenChange on-open-change
               :modal false}

       (when open? ($ :div {:class-name "fixed top-0 left-0 inset-0 z-50 bg-black/80 w-screen h-screen transition-opacity"}))

       ($ DialogContent {:class-name "max-w-[768px] lg:max-w-[1024px]"}
          ($ DialogHeader
             ($ DialogTitle
                (str "Edit " item-count " " (if (= item-count 1) "item" "items")))
             ($ DialogDescription
                "Select the fields you want to update for all selected items."))

          ($ Form (merge form)
             ($ :form {:id "edit-dialog-form"
                       :no-validate true
                       :on-submit (handle-submit on-submit on-invalid)}

                ($ :div {:class-name "space-y-2 py-2 z-[100]"}
                   ;; Render all update field rows
                   (for [[idx field] (map-indexed vector fields)]
                     ($ :div {:key (:id field)
                              :class-name "grid grid-cols-12 items-center gap-2"}

                        ;; Field selector
                        ($ Select {:value (:name field)
                                   :onValueChange #(handle-update-field % idx)}
                           ($ SelectTrigger {:class-name "col-span-4"}
                              ($ SelectValue))
                           ($ SelectContent
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
                      "Feld hinzufügen"))

                ($ DialogFooter {:class-name "pt-4"}
                   ($ Button {:type "button"
                              :variant "outline"
                              :on-click #(on-open-change false)}
                      "Abbrechen")
                   ($ Button {:type "submit"
                              :form "edit-dialog-form"
                              :disabled (zero? item-count)}
                      (str "Auf " item-count " "
                           (if (= item-count 1) "Gegenstand" "Gegenstände")
                           " anwenden")))))))))
