(ns leihs.inventory.client.components.patch-item-form
  (:require
   ["@@/badge" :refer [Badge]]
   ["@@/button" :refer [Button]]
   ["@@/form" :refer [Form]]
   ["@@/select" :refer [Select SelectContent SelectItem SelectTrigger
                        SelectValue]]
   ["@hookform/resolvers/zod" :refer [zodResolver]]
   ["date-fns" :refer [format]]
   ["lucide-react" :refer [Equal Trash CirclePlus UserLock]]
   ["react-hook-form" :as hook-form]
   ["react-i18next" :refer [useTranslation]]
   ["react-router" :refer [useParams]]
   ["zod" :as z]
   [clojure.string :as str]
   [leihs.inventory.client.components.typo :refer [Typo]]
   [leihs.inventory.client.lib.hooks :refer [use-dependent-fields]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.pools.inventory.search-edit.components.field-dispatcher
    :refer [FieldDispatcher]]
   [uix.core :as uix :refer [$ defui]]))

(defn- coerce-patch-field-value [field-name val]
  (let [formatted-val (if (instance? js/Date val)
                        (format val "yyyy-MM-dd")
                        val)
        coerced-val (cond
                      (= formatted-val "true") true
                      (= formatted-val "false") false
                      (= field-name "price")
                      (let [s (str formatted-val)]
                        (when-not (str/blank? s)
                          (let [n (js/parseFloat s)]
                            (when-not (js/isNaN n) n))))
                      :else formatted-val)
        final-val (if (and (object? coerced-val) (some? (.-value coerced-val)))
                    (.-value coerced-val)
                    coerced-val)]
    final-val))

;; Schema for a single update field entry
;; Transforms {name: "inventory_code", value: "ABC"} → {:inventory_code "ABC"}
(def update-field-schema
  (-> (z/object
       (cj {:name (z/string)
            :value (-> (z/any)
                       (.refine (fn [v] (some? v))))}))
      (.transform
       (fn [field]
         (let [final-val (coerce-patch-field-value (.-name field) (.-value field))]
           (when (some? final-val)
             (cj {(keyword (.-name field)) final-val})))))))

(def edit-dialog-schema
  (-> (z/object
       (cj {:update (z/array update-field-schema)}))
      (.transform
       (fn [data]
         (cj {:update (->> (jc (.-update data))
                           (remove nil?)
                           (remove #(contains? % :building_id))
                           cj)})))))

;; Main EditDialog Component
(defui PatchItemForm [{:keys [blocks on-submit on-invalid on-fields-change class-name initial-fields]}]
  (let [[t] (useTranslation)
        params (useParams)
        pool-id (aget params "pool-id")

        ;; Build an update row entry from a block (no default value)
        create-update-entry (fn [block]
                              (-> block
                                  (assoc :id (str (random-uuid)))
                                  (dissoc :label)
                                  (assoc :value (case (:component block)
                                                  "textarea" ""
                                                  "input" ""
                                                  "price" ""
                                                  "checkbox" []
                                                  nil))))

        initial-entries (if (seq initial-fields)
                          (->> initial-fields
                               (keep (fn [{:keys [name value]}]
                                       (when-let [block (->> blocks (filter #(= (:name %) name)) first)]
                                         (assoc (create-update-entry block) :value value))))
                               vec)
                          [{:id (str (random-uuid))}])

        form (hook-form/useForm
              #js {:resolver (zodResolver edit-dialog-schema)
                   :defaultValues (cj {:update initial-entries})})

        control (.-control form)
        field-array (hook-form/useFieldArray
                     (cj {:control control
                          :name "update"}))

        ;; Dependent fields hook manages retired→reason and building→room
        {:keys [fields has-reason-ref has-room-ref]}
        (use-dependent-fields {:field-array field-array
                               :control control
                               :pool-id pool-id
                               :name "update"})

        {:keys [append remove update]} (jc field-array)

        ;; Find field names already in use
        used-field-names (set (map :name fields))

        ;; Next available block (first unused)
        next-block (->> blocks
                        (filter #(not (contains? used-field-names (:name %))))
                        first)

        handle-add-field (fn []
                           (append (cj {:id (str (random-uuid))})))

        handle-update-field (fn [field-name idx]
                              (let [selected-block (->> blocks
                                                        (filter #(= (:name %) field-name))
                                                        first)]

                                (when selected-block
                                  (update idx (cj (create-update-entry selected-block))))))

        handle-remove (fn [idx]
                        (let [field (nth fields idx)
                              dep-name (cond
                                         (= (:name field) "retired") "retired_reason"
                                         (= (:name field) "building_id") "room_id"
                                         :else nil)
                              dep-idx (when dep-name
                                        (->> fields
                                             (map-indexed vector)
                                             (filter #(= (:name (second %)) dep-name))
                                             ffirst))]
                          (when dep-idx
                            (when (= dep-name "retired_reason")
                              (reset! has-reason-ref false))
                            (when (= dep-name "room_id")
                              (reset! has-room-ref false))
                            (remove dep-idx))
                          (remove idx)))

        handle-submit (.. form -handleSubmit)
        watched-update (jc (hook-form/useWatch (cj {:control control
                                                    :name "update"})))
        prev-fields-ref (uix/use-ref nil)]

    (uix/use-effect
     (fn []
       (let [current (mapv #(select-keys % [:name :value]) (or watched-update []))]
         (when (not= current @prev-fields-ref)
           (reset! prev-fields-ref current)
           (when on-fields-change
             (on-fields-change (or watched-update []))))))
     [watched-update on-fields-change])

    ($ Form (merge form)
       ($ :form {:id "patch-item-form"
                 :no-validate true
                 :on-submit (handle-submit on-submit on-invalid)
                 :class-name (str "space-y-2 px-2 border border-dashed rounded-lg " class-name)}

          ($ :div {:class-name "space-y-2 py-2"}
              ;; Render all update field rows
             (for [[idx field] (map-indexed vector fields)]
               (let [is-retired-reason (= (:name field) "retired_reason")
                     is-room (= (:name field) "room_id")]
                 ($ :div {:key (:id field)
                          :class-name "grid grid-cols-12 items-center gap-2"}

                    ;; Field selector (or static label for auto-managed fields)
                    (cond
                      is-retired-reason
                      ($ Typo {:variant "description"
                               :class-name "col-span-4 border border-border rounded-md p-2 shadow-sm"}
                         (t "fields.Status.retired_reason"))

                      is-room
                      ($ Typo {:variant "description"
                               :class-name "col-span-4 border border-border rounded-md p-2 shadow-sm"}
                         (t "fields.Location.room_id"))

                      :else
                      ($ Select {:value (or (:name field) "")
                                 :onValueChange #(handle-update-field % idx)}
                         ($ SelectTrigger {:data-test-id (str "field-select-" idx)
                                           :class-name "col-span-4"}
                            ($ SelectValue {:placeholder (t "pool.models.search_edit.select_field_placeholder")}))
                         ($ SelectContent {:data-test-id "field-options"}
                            (for [block blocks]
                              (when (and (not= (:name block) "retired_reason")
                                         (not= (:name block) "room_id"))
                                ($ SelectItem {:key (:name block)
                                               :disabled (and (not= (:name block) (:name field))
                                                              (contains? used-field-names (:name block)))
                                               :value (:name block)}

                                   ($ :button {:type "button"}
                                      ($ :div {:class-name "flex"}
                                         (:label block)
                                         (when (:owner_only block)
                                           ($ Badge {:class-name "px-1 ml-2"
                                                     :variant "secondary"}
                                              ($ UserLock {:class-name "w-3 h-3"})))))))))))

                    ($ Equal {:class-name (str "col-span-1 justify-self-center"
                                               (when (nil? (:name field)) " invisible"))})

                    ;; Value input (no default value)
                    ($ :div {:class-name "col-span-6"}
                       ($ FieldDispatcher {:form form
                                           :block (assoc field
                                                         :name (str "update." idx ".value"))}))

                    ;; Remove button (invisible for auto-managed fields)
                    ($ Button {:type "button"
                               :variant "outline"
                               :size "icon"
                               :on-click #(handle-remove idx)
                               :class-name (str "col-span-1 self-center justify-self-end "
                                                (when (or is-retired-reason is-room)
                                                  "invisible"))}
                       ($ Trash {:class-name "h-4 w-4"})))))

             ;; Add field button
             ($ Button {:type "button"
                        :variant "secondary"
                        :size "sm"
                        :on-click handle-add-field
                        :disabled (nil? next-block)
                        :class-name "border border-border"}

                ($ CirclePlus {:class-name "h-4 w-4"})
                (t "pool.models.search_edit.dialog.add_field")))))))
