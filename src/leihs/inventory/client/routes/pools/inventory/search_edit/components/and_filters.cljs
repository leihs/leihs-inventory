(ns leihs.inventory.client.routes.pools.inventory.search-edit.components.and-filters
  (:require
   ["@@/button" :refer [Button]]
   ["@@/select" :refer [Select SelectContent SelectItem SelectTrigger
                        SelectValue]]
   ["@@/toggle-group" :refer [ToggleGroup ToggleGroupItem]]
   ["lucide-react" :refer [ChevronLeft ChevronRight Equal Trash]]
   ["react-hook-form" :as hook-form]
   ["react-i18next" :refer [useTranslation]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.pools.inventory.search-edit.components.field-dispatcher
    :refer [FieldDispatcher]]
   [leihs.inventory.client.routes.pools.inventory.search-edit.components.or-context :refer [use-or]]
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
    {:operator "$ilike"}
    "autocomplete"
    {:operator "$eq"}
    "autocomplete-search"
    {:operator "$eq"}
    {:operator "$eq"}))

;; Main AndFilters Component
(defui AndFilters []
  (let [{:keys [index form blocks]} (use-or)

        ;; Compose name from context index
        name (str "$or." index ".$and")

        control (.-control form)
        {:keys [fields append remove update]}
        (jc (hook-form/useFieldArray
             (cj {:control control
                  :name name})))

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
                                 (update index (cj (assoc field :operator op))))

        handle-remove (fn [index]
                        (remove index))]

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
       (for [[index field] (map-indexed vector fields)]
         ($ :div {:key (:id field)
                  :class-name "grid grid-cols-12 items-center"}

            ;; Field selector dropdown
            ($ Select {:value (:name field)
                       :onValueChange #(handle-update-field % index)}
               ($ SelectTrigger {:class-name "col-span-3"}
                  ($ SelectValue))

               ($ SelectContent
                  (for [block blocks]
                    ($ SelectItem {:key (:name block)
                                   :disabled (and (not= (:name block) (:name field))
                                                  (let [occurrences (->> fields
                                                                         (filter #(= (:name %) (:name block)))
                                                                         count)
                                                        max-allowed (if (= (:component block) "calendar") 2 1)]
                                                    (>= occurrences max-allowed)))
                                   :value (:name block)}
                       ($ :button {:type "button"}
                          (t (:label block)))))))

            ;; Equals sign
            ($ ToggleGroup {:type "single"
                            :variant "outline"
                            :size "xs"
                            :class-name " col-span-2 self-center justify-self-center"
                            :value (:operator field)
                            :on-value-change (fn [op]
                                               (handle-update-operator op field index))}
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
                                                 :name (str name "." index ".value"))}))

            ;; Remove and filter button
            ($ Button {:type "button"
                       :variant "outline"
                       :size "icon"
                       :on-click #(handle-remove index)
                       :class-name "self-center justify-self-end"}
               ($ Trash {:class-name "h-4 w-4"}))))

       ;; Add and filter button (disabled if no more fields available)
       ($ Button {:type "button"
                  :variant "outline"
                  :size "sm"
                  :on-click handle-add-and
                  :disabled (nil? next-block)}
          "UND hinzufügen"))))
