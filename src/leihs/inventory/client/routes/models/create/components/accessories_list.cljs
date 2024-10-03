(ns leihs.inventory.client.routes.models.create.components.accessories-list
  (:require
   ["@/components/react/sortable-list" :refer [SortableList Draggable DragHandle]]
   ["@@/button" :refer [Button]]
   ["@@/card" :refer [Card]]
   ["@@/input" :refer [Input]]
   ["lucide-react" :refer [CirclePlus Trash]]
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

(defui main [props]
  (let [props (jc (cj props))
        [accessories set-accessories!] (uix/use-state [])
        [accessory set-accessory!] (uix/use-state "")]

    (uix/use-effect
     (fn []
       ((:onChange props) (cj (vec (map :name accessories))))
       (set-accessory! ""))
     [accessories set-accessory!])

    ($ :div {:className "flex flex-col gap-4"}
       ($ :div {:className "flex gap-4"}

          ($ Input {:id (:id props)
                    :ref (-> props :argv :ref)
                    :value accessory
                    :placeholer (:placeholder props)
                    :on-change #(set-accessory! (.. % -target -value))
                    :on-blur (:onBlur props)
                    :aria-invalid (:aria-invalid props)
                    :aria-describedby (::aria-describedby props)})

          ($ Button {:type "button"
                     :className ""
                     :disabled (empty? accessory)
                     :variant "outline"
                     :on-click #(set-accessories!
                                 (conj accessories
                                       {:id (js/crypto.randomUUID)
                                        :name accessory}))}

             ($ CirclePlus {:className "p-1"}) (:button props)))

       ($ SortableList {:items (cj (map :id accessories))
                        :onDragEnd #(handle-drag-end % set-accessories!)}

          (for [accessory accessories]
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
                                             accessories))}
                        ($ Trash {:className "p-1"}))))))))))

(def AccessoryList
  (uix/forward-ref
   (uix/as-react
    (fn [props]
      (main props)))))
