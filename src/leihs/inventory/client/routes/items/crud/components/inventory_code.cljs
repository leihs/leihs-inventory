(ns leihs.inventory.client.routes.items.crud.components.inventory-code
  (:require
   ["@/components/react/sortable-list" :refer [Draggable DragHandle
                                               SortableList]]
   ["@@/button" :refer [Button]]
   ["@@/form" :refer [FormControl FormField FormItem FormMessage FormLabel]]
   ["@@/input" :refer [Input]]
   ["@@/table" :refer [Table TableBody TableCell TableRow]]
   ["lucide-react" :refer [CirclePlus Trash]]
   ["react-hook-form" :as hook-form]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom :as uix-dom]))

(defui main [{:keys [control props]}]
  (let []

    ($ :div {:className "flex flex-col space-y-2"}
       ($ FormLabel "Inventarcode *")

       ($ :div {:className "flex flex-row gap-2"}
          ($ FormField
             {:control (cj control)
              :name (str "inventory-code")
              :render #($ FormItem {:class-name "flex-1"}
                          ($ FormControl
                             ($ Input (merge
                                       (:field (jc %))))))}

             ($ FormMessage))

          ($ Button {:type "button"
                     :variant "outline"}

             ($ CirclePlus {:className "w-4 h-4"}) "Zuletzt verwendet +1")))))

(def InventoryCode
  (uix/as-react
   (fn [props]
     (main props))))
