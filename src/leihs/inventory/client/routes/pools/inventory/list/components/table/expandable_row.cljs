(ns leihs.inventory.client.routes.pools.inventory.list.components.table.expandable-row
  (:require
   ["@@/button" :refer [Button]]
   ["@@/table" :refer [TableCell
                       TableRow]]
   ["lucide-react" :refer [Minus Plus]]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [onExpand subrows subrowCount
                     children className]
              :as props}]

  (let [attrs (dissoc props
                      :onExpand
                      :subrows
                      :subrowCount
                      :children
                      :className)]
    ($ :<>
       ($ TableRow (merge attrs
                          {:class-name className})

          ($ TableCell
             ($ :div {:className "flex items-center gap-4 ml-2"}
                ($ Button {:variant "outline"
                           :on-click onExpand
                           :size "icon"
                           :class-name (if
                                        (zero? subrowCount)
                                         "cursor-not-allowed"
                                         "")
                           :disabled (zero? subrowCount)}
                   (if subrows
                     ($ Minus {:className "h-4 w-4"})
                     ($ Plus {:className "h-4 w-4"})))

                ($ :span {:className "text-xl ml-auto"}
                   subrowCount)))

          children)
       ;; render subrows
       subrows)))

(def ExpandableRow
  (uix/as-react
   (fn [props]
     (main props))))
