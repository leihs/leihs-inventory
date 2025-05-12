(ns leihs.inventory.client.routes.models.crud.components.model-properties
  (:require
   ["@/components/react/sortable-list" :refer [SortableList Draggable DragHandle]]
   ["@@/button" :refer [Button]]
   ["@@/form" :refer [FormField FormItem FormControl FormMessage]]
   ["@@/input" :refer [Input]]
   ["@@/table" :refer [Table TableHeader TableRow TableHead TableBody TableCell]]
   ["@@/textarea" :refer [Textarea]]
   ["lucide-react" :refer [CirclePlus Trash]]
   ["react-hook-form" :as hook-form]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [defui $]]
   [uix.dom :as uix-dom]))

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

(defui properties-table [{:keys [children inputs]}]
  ($ Table
     ($ TableHeader
        ($ TableRow
           ($ TableHead "Eigenschaft")
           ($ TableHead "Wert")
           ($ TableHead "")))
     ($ TableBody children)))

(defui main [{:keys [control props]}]
  (let [{:keys [fields append remove move]} (jc (hook-form/useFieldArray
                                                 (cj {:control control
                                                      :name "properties"})))
        inputs (:inputs props)
        handle-add (fn []
                     (uix-dom/flush-sync
                      (append (cj {:key ""
                                   :value ""})))
                     (let [name (str "textarea[name='properties." (count fields) ".key']")
                           next (js/document.querySelector name)]
                       (when next (.focus next))))]

    ($ :div {:className "flex flex-col gap-2"}

       (when (not-empty fields)
         ($ :div {:className "rounded-md border"}

            ($ SortableList {:items (cj (map :id fields))
                             :onDragEnd (fn [e] (handle-drag-end e fields move))}
               ($ properties-table {:inputs inputs}
                  (doall
                   (map-indexed
                    (fn [index field]
                      ($ Draggable {:key (:id field)
                                    :id (:id field)
                                    :asChild true}

                         ($ TableRow {:key (:id field)}

                            ($ TableCell {:class-name "hidden"}
                               ($ FormField
                                  {:control (cj control)
                                   :name (str "properties." index ".id")
                                   :render #($ FormItem
                                               ($ FormControl
                                                  ($ Input (merge
                                                            {:className "min-h-[2.5rem]"}
                                                            (:field (jc %))))))}

                                  ($ FormMessage)))

                            ($ TableCell
                               ($ FormField
                                  {:control (cj control)
                                   :name (str "properties." index ".key")
                                   :render #($ FormItem
                                               ($ FormControl
                                                  ($ Textarea (merge
                                                               {:className "min-h-[2.5rem]"
                                                                :autoscale true
                                                                :resize true}
                                                               (:field (jc %))))))}

                                  ($ FormMessage)))

                            ($ TableCell
                               ($ FormField
                                  {:control (cj control)
                                   :name (str "properties." index ".value")
                                   :render #($ FormItem
                                               ($ FormControl
                                                  ($ Textarea (merge
                                                               {:className "min-h-[2.5rem]"
                                                                :autoscale true
                                                                :resize true}
                                                               (:field (jc %))))))}

                                  ($ FormMessage)))

                            ($ TableCell
                               ($ :div {:className "flex gap-2 justify-end"}
                                  #_($ DragHandle {:id (:id field)
                                                   :className "cursor-move"})

                                  ($ Button {:variant "outline"
                                             :size "icon"
                                             :className "cursor-pointer"
                                             :on-click #(remove index)}
                                     ($ Trash {:className "w-4 h-4"})))))))
                    fields))))))

       ($ :div {:className "flex"}
          ($ Button {:type "button"
                     :className ""
                     :variant "outline"
                     :on-click handle-add}

             ($ CirclePlus {:className "w-4 h-4"}) "Eigenschaft hinzufügen")))))

(def ModelProperties
  (uix/as-react
   (fn [props]
     (main props))))
