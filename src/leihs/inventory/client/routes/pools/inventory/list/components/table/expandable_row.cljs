(ns leihs.inventory.client.routes.pools.inventory.list.components.table.expandable-row
  (:require
   ["@@/button" :refer [Button]]
   ["@@/table" :refer [TableCell
                       TableRow]]
   ["lucide-react" :refer [Minus Plus Loader2Icon]]
   [uix.core :as uix :refer [$ defui]]))

(defui ExpandableRow [{:keys [on-expand subrows subrow-count
                              children class-name]
                       :as props}]

  (let [attrs (dissoc props
                      :on-expand
                      :subrows
                      :subrow-count
                      :children
                      :class-name)

        [loading set-loading!] (uix/use-state false)
        handle-expand (fn []
                        (when (not subrows)
                          (set-loading! true))

                        (when on-expand
                          (on-expand)))]

    (uix/use-effect
     (fn []
       (when subrows
         (set-loading! false)))
     [subrows])

    ($ :<>
       ($ TableRow (merge attrs
                          {:class-name class-name})

          ($ TableCell
             ($ :div {:class-name "flex items-center gap-4 ml-2"}
                ($ Button {:variant "outline"
                           :data-test-id "expand-button"
                           :on-click handle-expand
                           :size "icon"
                           :class-name (cond
                                         (or (nil? subrow-count)
                                             (zero? subrow-count))
                                         "invisible"
                                         :else
                                         "")}
                   (if loading
                     ($ Loader2Icon {:class-name "h-4 w-4 animate-spin"})
                     (if subrows
                       ($ Minus {:class-name "h-4 w-4"})
                       ($ Plus {:class-name "h-4 w-4"}))))

                ($ :span {:class-name "text-xl ml-auto w-6 text-right"
                          :data-test-id "items"}
                   subrow-count)))

          children)
       ;; render subrows
       subrows)))

