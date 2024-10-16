(ns leihs.inventory.client.routes.models.create.components.model-properties
  (:require
   ["@/components/react/sortable-list" :refer [SortableList Draggable DragHandle]]
   ["@@/button" :refer [Button]]
   ["@@/card" :refer [Card]]
   ["@@/form" :refer [FormField FormItem FormLabel FormControl FormDescription FormMessage]]
   ["@@/input" :refer [Input]]
   ["lucide-react" :refer [CirclePlus Trash]]
   ["react-hook-form" :as hook-form]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [defui $]]
   [uix.dom]))

(defn find-index-by-id [vec id]
  (some (fn [[idx item]]
          (when (= (:id item) id)
            idx))
        (map-indexed vector vec)))

(defn move-element [arr old-index new-index]
  (let [elem (nth arr old-index)
        arr-without-elem (vec (concat (subvec arr 0 old-index)
                                      (subvec arr (inc old-index))))
        new-arr (vec (concat (subvec arr-without-elem 0 new-index)
                             [elem]
                             (subvec arr-without-elem new-index)))]
    new-arr))

(defn handle-drag-end [event set-accessories!]
  (let [ev (jc event)]
    (when-not (= (-> ev :over :id)
                 (-> ev :active :id))

      (set-accessories!
       (fn [accessories]
         (let [old-index (find-index-by-id accessories
                                           (-> ev :active :id))
               new-index (find-index-by-id accessories
                                           (-> ev :over :id))]

           (move-element accessories old-index new-index)))))))

(defui main [{:keys [control props]}]
  (let [{:keys [fields append]} (jc (hook-form/useFieldArray
                                     (cj {:control control
                                          :name "properties"})))

        [properties set-accessories!] (uix/use-state [])
        [accessory set-accessory!] (uix/use-state "")]

    (js/console.debug fields append)
    ;; (uix/use-effect
    ;;  (fn []
    ;;    ((:onChange props) (cj (vec (map :name properties))))
    ;;    (set-accessory! ""))
    ;;  [properties set-accessory!])

    #_(map-indexed
       (fn [index field]
    ;; Your code here, using `index` and `field`
         )
       fields)

    ($ :div {:className "flex flex-col gap-2"}
       (map-indexed
        (fn [index field]
          ($ FormField {:control (cj control)
                        :key (:id field)
                        :name (str "properties." index ".name")
                        :render #($ FormItem {:class-name ""}
                                    ($ FormControl
                                       ($ Input (merge
                                                 (:field (jc %))))))}

             ($ FormMessage))
          fields))

       ($ :div {:className "flex"}
          ($ Button {:type "button"
                     :className ""
                     :variant "outline"
                     :on-click #(append (cj {:name "New"}))}

             ($ CirclePlus {:className "p-1"}) (-> props :button)))

       ($ SortableList {:items (cj (map :id properties))
                        :onDragEnd #(handle-drag-end % set-accessories!)}

          (for [accessory properties]
            ($ Draggable {:key (:id accessory)
                          :id (:id accessory)}

               ($ Card {:className "flex px-4 py-2 items-center"}
                  ($ :p {:className "text-[0.85rem] font-medium leading-snug"}
                     (:name accessory))

                  ($ :div {:className "ml-auto flex gap-2"}
                     ($ DragHandle {:id (:id accessory)
                                    :className "cursor-move"})

                     ($ Button {:variant "outline"
                                :size "icon"
                                :className "cursor-pointer"
                                :on-click #(set-accessories!
                                            (filter
                                             (fn [el] (not= (:id el)
                                                            (:id accessory)))
                                             properties))}
                        ($ Trash {:className "p-1"}))))))))))

(def ModelProperties
  (uix/forward-ref
   (uix/as-react
    (fn [props]
      (main props)))))
