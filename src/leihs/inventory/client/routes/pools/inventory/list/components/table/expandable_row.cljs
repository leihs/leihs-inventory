(ns leihs.inventory.client.routes.pools.inventory.list.components.table.expandable-row
  (:require
   ["@@/button" :refer [Button]]
   ["@@/table" :refer [TableCell
                       TableRow]]
   ["lucide-react" :refer [ChevronDown ChevronRight Loader2Icon]]
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
             ($ :div {:class-name "flex items-center ml-2"}
                (if (and subrow-count (pos? subrow-count))
                  ($ Button {:variant "outline"
                             :data-test-id "expand-button"
                             :on-click handle-expand
                             :size "sm"
                             :class-name "w-[68px] justify-between px-2.5 text-sm font-semibold"}
                     (if loading
                       ($ Loader2Icon {:class-name "h-4 w-4 animate-spin"})
                       (if subrows
                         ($ ChevronDown {:class-name "h-4 w-4"})
                         ($ ChevronRight {:class-name "h-4 w-4"})))
                     ($ :span {:data-test-id "items"}
                        subrow-count))

                  ($ :span {:class-name "w-[68px] px-2.5 text-sm font-semibold text-right"
                            :data-test-id "items"}
                     subrow-count))))

          children)
       ;; render subrows
       subrows)))

