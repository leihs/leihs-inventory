(ns leihs.inventory.client.routes.pools.inventory.search-edit.components.filters.and-filters
  (:require
   ["@@/button" :refer [Button]]
   ["@@/select" :refer [Select SelectContent SelectItem SelectTrigger
                        SelectValue]]
   ["@@/toggle-group" :refer [ToggleGroup ToggleGroupItem]]
   ["lucide-react" :refer [ChevronLeft ChevronRight Equal Trash CirclePlus]]
   ["react-hook-form" :as hook-form]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [useParams]]

   [leihs.inventory.client.components.form.fields.common-field :refer [CommonField]]

   [leihs.inventory.client.components.typo :refer [Typo]]
   [leihs.inventory.client.lib.hooks :refer [use-dependent-fields]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.pools.inventory.search-edit.components.field-dispatcher :refer [FieldDispatcher]]
   [leihs.inventory.client.routes.pools.inventory.search-edit.components.filters.or-context :refer [use-or]]
   [uix.core :as uix :refer [$ defui]]))

(defn default-operator [block]
  (case (:component block)
    "calendar"
    {:operator "$eq"}
    "checkbox"
    {:operator "$eq"}
    "radio-group"
    {:operator "$eq"}
    "select"
    {:operator "$eq"}
    "textarea"
    {:operator "$ilike"}
    "input"
    (if (= (:name block) "price")
      {:operator "$eq"}
      {:operator "$ilike"})
    "autocomplete"
    {:operator "$eq"}
    "autocomplete-search"
    {:operator "$eq"}
    {:operator "$eq"}))

(defui AndFilters []
  (let [{:keys [index form blocks]} (use-or)
        params (router/useParams)

        pool-id (aget params "pool-id")

        ;; Compose name from context index
        name (str "$or." index ".$and")
        get-values (aget form "getValues")

        control (.-control form)
        field-array (hook-form/useFieldArray
                     (cj {:control control
                          :keyName "and-filter-id"
                          :name name}))

        ;; Dependent fields hook manages retired→reason and building→room
        {:keys [fields has-reason-ref has-room-ref]}
        (use-dependent-fields {:field-array field-array
                               :control control
                               :pool-id pool-id
                               :name name
                               :extra-field-props {:allowed-operators ["$ilike"]
                                                   :operator "$ilike"}})

        {:keys [append remove update]} (jc field-array)

        [t] (useTranslation)
        init (uix/use-ref false)

        ;; Find next available block
        used-field-names (set (map :name fields))
        next-block (->> blocks
                        (filter #(not (contains? used-field-names (:name %))))
                        first)

        ;; Create initial and filter structure
        create-and-filter (fn [block]
                            (-> block
                                (assoc :id (str (random-uuid)))
                                (dissoc :label)
                                (merge (default-operator block))))

        handle-add-and (fn []
                         (when next-block
                           (append (cj (create-and-filter next-block)))))

        handle-update-field (fn [field-name index]
                              (let [selected-block (->> blocks
                                                        (filter #(= (:name %) field-name))
                                                        first)]

                                (when selected-block
                                  (update index (cj (create-and-filter selected-block))))))

        handle-update-operator (fn [op field index]
                                 (let [current-val (get-values (str name "." index ".value"))]
                                   (update index (cj (assoc field :operator op :value current-val)))))

        handle-remove (fn [index]
                        ;; If removing a parent field, also remove its dependent
                        (let [field (nth fields index)
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
                          (remove index)))]

    (uix/use-effect
     (fn []
       (when (and (not @init)
                  (empty? fields)
                  (first blocks))
         (reset! init true)
         (handle-add-and)))
     [fields blocks handle-add-and])

    ($ :<>
        ;; Render all and filter items
       (for [[field-index field] (map-indexed vector fields)]
         (let [is-retired-reason (= (:name field) "retired_reason")
               is-room (= (:name field) "room_id")]

           ($ :<> {:key (:id field)}
              ($ :div {:class-name "grid grid-cols-12 items-center"}

                 (cond
                   is-retired-reason
                   ($ Typo {:variant "description"
                            :class-name "col-span-3 border border-border rounded-md p-2 shadow-sm"}
                      (t "fields.Status.retired_reason"))

                   is-room
                   ($ Typo {:variant "description"
                            :class-name "col-span-3 border border-border rounded-md p-2 shadow-sm"}
                      (t "fields.Location.room_id"))

                   :else
                    ;; Field selector dropdown
                   ($ Select {:value (:name field)
                              :disabled is-retired-reason
                              :onValueChange #(handle-update-field % field-index)}
                      ($ SelectTrigger {:data-test-id (str "or-" index "-field-select-" field-index)
                                        :class-name "col-span-3"}
                         ($ SelectValue))

                      ($ SelectContent {:data-test-id "field-options"}
                         (for [block blocks]
                           (when (and (not= (:name block) "retired_reason")
                                      (not= (:name block) "room_id"))
                             ($ SelectItem {:key (:name block)
                                            :disabled (and
                                                       (not= (:name block)
                                                             (:name field))

                                                       (let [occurrences (->> fields
                                                                              (filter #(= (:name %) (:name block)))
                                                                              count)
                                                             max-allowed (if (= (:component block) "calendar") 2 1)]
                                                         (>= occurrences max-allowed)))
                                            :value (:name block)}
                                ($ :button {:type "button"}
                                   (t (:label block)))))))))

                 ;; Equals sign
                 ($ ToggleGroup {:type "single"
                                 :variant "outline"
                                 :size "xs"
                                 :class-name " col-span-2 self-center justify-self-center"
                                 :value (:operator field)
                                 :on-value-change (fn [value]
                                                    (handle-update-operator value field field-index))}
                    (for [op (:allowed-operators field)]
                      ($ ToggleGroupItem {:key op
                                          :disabled (when
                                                     (= (count (:allowed-operators field)) 1)
                                                      true)
                                          :value op}
                         (case op
                           "$ilike" ($ Equal)
                           "$eq" ($ Equal)
                           "$gte" ($ ChevronRight)
                           "$lte" ($ ChevronLeft)
                           ($ Equal)))))

                 ;; Field input (dynamic based on field type)
                 ($ :div {:class-name "col-span-6"}
                    ($ FieldDispatcher {:form form
                                        :block (assoc field
                                                      :name (str name "." field-index ".value"))}))

                 ;; Remove and filter button (hidden for auto-managed retired_reason)
                 ($ Button {:data-test-id (str "remove-and-" field-index)
                            :type "button"
                            :variant "outline"
                            :size "icon"
                            :on-click #(handle-remove field-index)
                            :class-name (str "self-center justify-self-end "
                                             (cond
                                               is-retired-reason
                                               "invisible"
                                               is-room
                                               "invisible"))}
                    ($ Trash {:class-name "h-4 w-4"}))))))

        ;; Add and filter button (disabled if no more fields available)
       ($ Button {:type "button"
                  :variant "secondary"
                  :size "sm"
                  :on-click handle-add-and
                  :disabled (nil? next-block)
                  :class-name "border border-border"}
          ($ CirclePlus {:class-name "h-4 w-4"})
          (t "pool.models.search_edit.add_and")))))
