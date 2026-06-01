(ns leihs.inventory.client.routes.pools.inventory.list.components.table.list-edit-actions
  (:require
   ["@@/button" :refer [Button]]
   ["@@/button-group" :refer [ButtonGroup]]
   ["lucide-react" :refer [ChevronDown]]
   ["react-i18next" :refer [useTranslation]]
   [uix.core :as uix :refer [$ defui]]))

(defn manage-model-timeline-url [pool-id model-id]
  (str "/manage/" (str pool-id) "/models/" (str model-id) "/timeline"))

(defui EditActionsPlaceholder []
  (let [[t] (useTranslation)]
    ($ ButtonGroup {:class-name "invisible pointer-events-none"
                    :aria-hidden true}
       ($ Button {:variant "outline"}
          (t "pool.models.list.actions.edit"))
       ($ Button {:variant "outline"
                  :size "icon"}
          ($ ChevronDown {:className "w-4 h-4"})))))

(defui TimelineActions [{:keys [pool-id model-id]}]
  (let [[t] (useTranslation)]
    ($ ButtonGroup
       ($ Button {:variant "outline"
                  :asChild true}
          ($ :a {:href (manage-model-timeline-url pool-id model-id)
                 :data-test-id "timeline-button"
                 :target "_blank"
                 :rel "noopener noreferrer"}
             (t "pool.models.list.actions.timeline")))
       ($ Button {:variant "outline"
                  :size "icon"
                  :class-name "invisible pointer-events-none"
                  :aria-hidden true
                  :tabIndex -1}
          ($ ChevronDown {:className "w-4 h-4"})))))
