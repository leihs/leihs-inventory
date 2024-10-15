(ns leihs.inventory.client.routes.models.create.components.model-properties
  (:require
   ["@/components/react/sortable-list" :refer [SortableList Draggable DragHandle]]
   ["@@/button" :refer [Button]]
   ["@@/form" :refer [FormField FormItem FormControl FormMessage]]
   ["@@/table" :refer [Table TableHeader TableRow TableHead TableBody TableCell]]
   ["@@/textarea" :refer [Textarea]]
   ["lucide-react" :refer [CirclePlus Trash]]
   ["react-hook-form" :as hook-form]
   [leihs.inventory.client.components.basic-form-field :as basic-form-field]

   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [defui $]]
   [uix.dom]))

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

(defui properties-table [{:keys [children fields-array]}]
  (js/console.debug fields-array "in head")
  ($ Table
     ($ TableHeader
        ($ TableRow
           ($ TableHead (-> fields-array (nth 0) :label))
           ($ TableHead (-> fields-array (nth 1) :label))
           ($ TableHead "Actions")))
     ($ TableBody children)))

;; soll weg, ist nun in textarea"

;; (defui autoscale-textarea [props]
;;   (let [ref (-> props :argv :ref)
;;         [height set-height!] (uix/use-state 0)
;;         [focused set-focused!] (uix/use-state false)
;;         textarea-ref (uix/use-ref nil)]
;;
;;     (uix/use-imperative-handle
;;      ref
;;      (fn []
;;        (.. textarea-ref -current))
;;      [textarea-ref])
;;
;;     (uix/use-effect
;;      (fn []
;;        (when (.. textarea-ref -current)
;;
;;          (if focused
;;            (set-height! (str (.. textarea-ref -current -scrollHeight) "px"))
;;            (set-height! "2.5rem")))
;;
;;        [props focused]))
;;
;;     ($ Textarea (merge
;;                  {:ref textarea-ref
;;                   :id (:id props)
;;                   :name (:name props)
;;                   :onChange (:onChange props)
;;                   :onBlur #(set-focused! false)
;;                   :onFocus #(set-focused! true)
;;                   :value (:value props)
;;                   :aria-invalid (:aria-invalid props)
;;                   :aria-describedby (:aria-describedby props)
;;                   :style {:height height}
;;                   :className "min-h-[2.5rem] resize-none 
;;                               overflow-hidden w-full transition-all 
;;                               duration-200"}))))
;;
;; (def AutoscaleTextarea
;;   (uix/forward-ref
;;    (uix/as-react
;;     (fn [props]
;;       (autoscale-textarea props)))))

(defui main [{:keys [control props]}]
  (let [{:keys [fields append remove move]} (jc (hook-form/useFieldArray
                                                 (cj {:control control
                                                      :name "properties"})))
        inputs (:fieldsArray props)]
    (js/console.debug inputs)

    ($ :div {:className "flex flex-col gap-2"}

       (when (not-empty fields)
         ($ :div {:className "rounded-md border"}

            ($ SortableList {:items (cj (map :id fields))
                             :onDragEnd (fn [e] (handle-drag-end e fields move))}
               ($ properties-table {:fields-array inputs}
                  (doall
                   (map-indexed
                    (fn [index field]
                      ($ Draggable {:key (:id field)
                                    :id (:id field)
                                    :asChild true}

                         ($ TableRow {:key (:id field)}
                            (for [input inputs]
                              ($ TableCell {:key (:name input)}
                                 ($ basic-form-field/main
                                    {:control control
                                     :input input
                                     :label false
                                     :class-name "min-h-[2.5rem]"
                                     :description false
                                     :name (str "properties." index (:name input))})))

                            ;; wird durch basic-form-field ersetzt

;; ($ FormField
                             ;;    {:control (cj control)
                             ;;     :name (str "properties." index ".name")
                             ;;     :render #($ FormItem {:class-name ""}
                             ;;                 ($ FormControl
                             ;;                    ($ Textarea (merge
                             ;;                                 {:className "min-h-[2.5rem]"
                             ;;                                  :autoscale true
                             ;;                                  :resize false}
                             ;;                                 (:field (jc %))))))}
                             ;;
                             ;;    ($ FormMessage)))

                            ;; ($ TableCell
                            ;;    ($ FormField
                            ;;       {:control (cj control)
                            ;;        :name (str "properties." index ".value")
                            ;;        :render #($ FormItem {:class-name ""}
                            ;;                    ($ FormControl
                            ;;                       ($ Textarea (merge
                            ;;                                    {:className "min-h-[2.5rem]"
                            ;;                                     :autoscale true
                            ;;                                     :resize false}
                            ;;                                    (:field (jc %))))))}
                            ;;
                            ;;       ($ FormMessage)))

                            ($ TableCell
                               ($ :div {:className "ml-auto flex gap-2"}
                                  ($ DragHandle {:id (:id field)
                                                 :className "cursor-move"})

                                  ($ Button {:variant "outline"
                                             :size "icon"
                                             :className "cursor-pointer"
                                             :on-click #(remove index)}
                                     ($ Trash {:className "p-1"})))))))
                    fields))))))

       ($ :div {:className "flex"}
          ($ Button {:type "button"
                     :className ""
                     :variant "outline"
                     :on-click #(append (cj {:name "" :value ""}))}

             ($ CirclePlus {:className "p-1"}) (-> props :button))))))

(def ModelProperties
  (uix/as-react
   (fn [props]
     (main props))))
