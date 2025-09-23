(ns leihs.inventory.client.routes.pools.inventory.list.components.table.expandable-row
  (:require
   ["@@/button" :refer [Button]]
   ["@@/table" :refer [TableCell
                       TableRow]]
   ["lucide-react" :refer [Minus Plus Loader2Icon]]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [onExpand subrows subrowCount
                     children className]
              :as props}]

  (let [attrs (dissoc props
                      :onExpand
                      :subrows
                      :subrowCount
                      :children
                      :className)

        [loading set-loading!] (uix/use-state false)
        handle-expand (fn []
                        (when (not subrows)
                          (set-loading! true))

                        (when onExpand
                          (onExpand)))]

    (uix/use-effect
     (fn []
       (when subrows
         (set-loading! false)))
     [subrows])

    ($ :<>
       ($ TableRow (merge attrs
                          {:class-name className})

          ($ TableCell
             ($ :div {:className "flex items-center gap-4 ml-2"}
                ($ Button {:variant "outline"
                           :on-click handle-expand
                           :size "icon"
                           :class-name (if
                                        (zero? subrowCount)
                                         "cursor-not-allowed"
                                         "")
                           :disabled (zero? subrowCount)}
                   (if loading
                     ($ Loader2Icon {:className "h-4 w-4 animate-spin"})
                     (if subrows
                       ($ Minus {:className "h-4 w-4"})
                       ($ Plus {:className "h-4 w-4"}))))

                ($ :span {:className "text-xl ml-auto w-6 text-right"}
                   subrowCount)))

          children)
       ;; render subrows
       subrows)))

(def ExpandableRow
  (uix/as-react
   (fn [props]
     (main props))))
