(ns leihs.inventory.client.routes.models.create.components.category-assignment
  (:require
   ["@/components/react/sortable-list" :refer [SortableList Draggable DragHandle]]
   ["@@/button" :refer [Button]]
   ["@@/form" :refer [FormField FormItem FormControl FormMessage]]
   ["@@/input" :refer [Input]]
   ["@@/table" :refer [Table TableHeader TableRow TableHead TableBody TableCell]]
   ["lucide-react" :refer [CirclePlus Trash]]
   ["react-hook-form" :as hook-form]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.models.create.context :refer [state-context]]
   [uix.core :as uix :refer [defui $]]
   [uix.dom]))

(defui main [{:keys [control props]}]
  (let [{:keys [categories]} (uix/use-context state-context)]

    (js/console.debug "categories" categories)
    ($ :div "hello")))

(def CategoryAssignment
  (uix/as-react
   (fn [props]
     (main props))))
