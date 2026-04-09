(ns leihs.inventory.client.routes.pools.inventory.search-edit.components.or-filters
  (:require
   ["@@/button" :refer [Button]]
   ["lucide-react" :refer [Trash]]
   ["react-hook-form" :as hook-form]
   ["react-i18next" :refer [useTranslation]]
   [leihs.inventory.client.components.typo :refer [Typo]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.pools.inventory.search-edit.components.and-filters :refer [AndFilters]]
   [leihs.inventory.client.routes.pools.inventory.search-edit.components.or-context :refer [OrProvider]]
   [uix.core :as uix :refer [$ defui]]))

;; Main OrFilters Component
(defui OrFilters [{:keys [blocks form children]}]
  (let [control (.-control form)
        [t] (useTranslation)
        {:keys [fields append remove]}
        (jc (hook-form/useFieldArray
             (cj {:control control
                  :name "$or"})))

        ;; Create initial or filter structure
        create-or-filter (fn []
                           {:id (str (random-uuid))
                            :$and []})

        handle-add-or (fn []
                        (append (clj->js (create-or-filter))))

        handle-remove (fn [index]
                        (remove index))]

    ($ :<>
       (if (empty? fields)
         ($ :button {:class-name "border border-dashed rounded p-8 w-full"
                     :type "button"
                     :on-click handle-add-or}
             ($ Typo {:variant "description"
                      :class-name "text-center"}
                (t "pool.models.search_edit.add_filter_prompt")))

         ;; Render all or filter items
         (for [[index field] (map-indexed vector fields)]
           ($ :div {:key (:id field)
                    :class-name "flex gap-2"}
              ($ :div {:class-name "flex flex-1 flex-col "}
                  (when (> index 0)
                    ($ :div {:class-name "text-xs font-medium text-center border bg-muted p-1 rounded-md mb-2"}
                       (t "pool.models.search_edit.or_separator")))
                   ;; And filters container
                 ($ :div {:class-name "space-y-2 border border-dashed rounded p-2 flex-1"}
                    ($ OrProvider {:index index
                                   :form form
                                   :blocks blocks}
                       children)))

              ;; Remove or filter button
              ($ Button {:type "button"
                         :variant "outline"
                         :size "icon"
                         :on-click #(handle-remove index)
                         :class-name "self-end"}
                 ($ Trash {:class-name "h-4 w-4"})))))

        ;; Add or filter button
        ($ Button {:type "button"
                   :variant "outline"
                   :size "sm"
                   :on-click handle-add-or}
           (t "pool.models.search_edit.add_or")))))
