(ns leihs.inventory.client.routes.items.crud.components.inventory-code
  (:require
   ["@@/button" :refer [Button]]
   ["@@/form" :refer [FormControl FormField FormItem FormLabel FormMessage]]
   ["@@/input" :refer [Input]]
   ["lucide-react" :refer [CirclePlus]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [$ defui]]))

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
