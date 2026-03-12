(ns leihs.inventory.client.components.form.form-field-array
  (:require
   ["@/components/react/sortable-list" :refer [SortableList Draggable DragHandle]]
   ["@@/button" :refer [Button]]
   ["@@/table" :refer [Table TableBody TableCell TableRow]]
   ["lucide-react" :refer [Trash]]
   ["react" :as react]
   ["react-hook-form" :as hook-form]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [$ defui]]))

(defn find-index-by-id [vec id]
  (some (fn [[idx item]]
          (when (= (:id item) id)
            idx))
        (map-indexed vector vec)))

(defn handle-drag-end [event fields move]
  (let [ev (jc event)]
    (when-not (= (-> ev :over :id)
                 (-> ev :active :id))

      (let [old-index (find-index-by-id fields
                                        (-> ev :active :id))
            new-index (find-index-by-id fields
                                        (-> ev :over :id))]

        (move old-index new-index)))))

;; Provider-level context for sharing useFieldArray instance
(def form-field-array-context
  (react/createContext #js {}))

;; Hook to access shared useFieldArray instance
(defn use-array-items []
  (let [context (react/useContext form-field-array-context)]
    (when (undefined? context)
      (throw (js/Error. "use-fields must be used within a FormFieldArray component")))
    {:fields (jc (.-fields context))
     :append (.-append context)
     :move (.-move context)
     :remove (.-remove context)
     :update (.-update context)}))

;; Row-level context for sharing field data with cells components
(def field-array-context
  (react/createContext #js {}))

;; Hook to access current field/index/update/remove
(defn use-array-item []
  (let [context (react/useContext field-array-context)]
    (when (undefined? context)
      (throw (js/Error. "use-field must be used within a FieldArray component")))
    {:form (.-form context)
     :name (.-name context)
     :field (jc (.-field context))
     :index (.-index context)
     :update (.-update context)
     :remove (.-remove context)}))

;; Provider component - calls useFieldArray ONCE
(defui form-field-array [{:keys [form name children]}]
  (let [control (cj (.-control form))
        {:keys [fields append move remove update]} (jc (hook-form/useFieldArray
                                                        (cj {:control control
                                                             :keyName (str name "-id")
                                                             :name name})))]
    ($ (.-Provider form-field-array-context)
       {:value #js {:fields fields
                    :append append
                    :remove remove
                    :move move
                    :update update}}

       ($ :div {:class-name "flex flex-col gap-2"}
          children))))

;; Table component - renders fields table
(defui form-array-fields [{:keys [form name sortable header children]
                           :or {sortable false
                                header nil}}]
  (let [{:keys [fields remove move update]} (use-array-items)
        key-field (keyword (str name "-id"))]

    (when (not-empty fields)
      ($ :div {:class-name "rounded-md border overflow-hidden"}
         (if sortable
           ($ SortableList {:items (cj (map :id fields))
                            :onDragEnd (fn [e] (handle-drag-end e fields move))}
              ($ Table {:class-name "w-full"}
                 (when header header)
                 ($ TableBody
                    (doall
                     (map-indexed
                      (fn [index field]
                        (let [ctx-value #js {:form form
                                             :name name
                                             :field field
                                             :index index
                                             :update update
                                             :remove remove}]
                          ($ (.-Provider field-array-context)
                             {:value ctx-value
                              :key (get field key-field)}

                             ($ Draggable {:key (:id field)
                                           :id (:id field)
                                           :asChild true}
                                ($ TableRow {:class-name ""}
                                   children
                                   ($ TableCell {:class-name "text-right"}
                                      (when sortable
                                        ($ DragHandle {:id (:id field)
                                                       :className "cursor-move mr-2"}))
                                      ($ Button {:variant "outline"
                                                 :type "button"
                                                 :on-click #(remove index)
                                                 :size "icon"}
                                         ($ Trash {:class-name "h-4 w-4"}))))))))
                      fields)))))

           ($ Table {:class-name "w-full"}
              (when header header)
              ($ TableBody
                 (doall
                  (map-indexed
                   (fn [index field]
                     (let [ctx-value #js {:form form
                                          :name name
                                          :field field
                                          :index index
                                          :update update
                                          :remove remove}]
                       ($ (.-Provider field-array-context)
                          {:value ctx-value
                           :key (get field key-field)}

                          ($ TableRow {:class-name ""}
                             children
                             ($ TableCell {:class-name "text-right"}
                                ($ Button {:variant "outline"
                                           :type "button"
                                           :on-click #(remove index)
                                           :size "icon"}
                                   ($ Trash {:class-name "h-4 w-4"})))))))
                   fields)))))))))

;; React-compatible exports
(def FormFieldArray
  (uix/as-react
   (fn [props]
     (form-field-array props))))

(def FormFieldArrayItems
  (uix/as-react
   (fn [props]
     (form-array-fields props))))
