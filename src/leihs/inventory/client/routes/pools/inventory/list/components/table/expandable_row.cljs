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
  (let []
    ($ :<>
       ($ TableRow (merge props
                          {:data-row "expandable"
                           :class-name className
                           :style (if subrows
                                    {:box-shadow
                                     "0 -0.5px 0 hsl(var(--border)),
                              0 4px 4px -2px hsl(var(--border))"}
                                    {:box-shadow "0 -0.5px 0 hsl(var(--border))"})})

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
