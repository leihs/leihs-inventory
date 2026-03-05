(ns leihs.inventory.client.routes.pools.inventory.search-edit.components.filters.retired-reason-field
  (:require
   ["lucide-react" :refer [ChevronLeft ChevronRight Equal]]

   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.pools.inventory.search-edit.components.field-dispatcher :refer [FieldDispatcher]]
   [uix.core :as uix :refer [$ defui]]))

(defui RetiredReasonField [{:keys [form name has-reason-ref field-index remove]}]
  (let [get-field-state ^js (.-getFieldState form)
        unregister ^js (.-unregister form)
        register ^js (.-register form)
        set-value ^js (.-setValue form)]

    ;; (uix/use-effect
    ;;  (fn []
    ;;    (reset! has-reason-ref true)
    ;;    (js/console.debug "mount" @has-reason-ref)
    ;;
    ;;    ;; Unregister the field on unmount
    ;;    (fn []
    ;;      (do
    ;;        (js/console.debug "unmount" @has-reason-ref)
    ;;        (reset! has-reason-ref false)
    ;;        #_(unregister (str name "." field-index)))))
    ;;  [has-reason-ref])

    (uix/use-effect
     (fn []
         ;; Register the field on mount

       (js/console.debug "Registering field:" name)
       (reset! has-reason-ref true)
       (set-value (str name "." (inc field-index))
                  (cj {:name "retired_reason"
                       :value ""
                       :operator "$ilike"}))

       ;; Unregister the field on unmount
       (fn []
         (do
           (js/console.debug "Unregistering field:" name field-index)
           (unregister (str name "." (inc field-index)))
           (reset! has-reason-ref false))))
     [has-reason-ref name field-index set-value unregister remove])

    ($ :div {:class-name "grid grid-cols-12 items-center"}
       ($ :div {:class-name "col-span-3"}
          "hello")
       ($ Equal {:class-name "col-span-2 self-center justify-self-center"})

       ($ :div {:class-name "col-span-6"}
          ($ FieldDispatcher {:form form
                              :block {:name (str name "." (inc field-index) ".value")
                                      :component "textarea"
                                      :id (str (random-uuid))}})))))
