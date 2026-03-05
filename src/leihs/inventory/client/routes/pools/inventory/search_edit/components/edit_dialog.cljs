(ns leihs.inventory.client.routes.pools.inventory.search-edit.components.edit-dialog
  (:require
   ["@@/alert" :refer [Alert AlertDescription AlertTitle]]
   ["@@/button" :refer [Button]]
   ["@@/dialog" :refer [Dialog DialogContent DialogDescription DialogFooter
                        DialogHeader DialogTitle]]
   ["@@/spinner" :refer [Spinner]]
   ["lucide-react" :refer [Info]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :refer [useFetcher]]
   ["sonner" :refer [toast]]
   [leihs.inventory.client.components.patch-item-form :refer [PatchItemForm]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [$ defui]]))

;; Main EditDialog Component
(defui EditDialog [{:keys [open? on-open-change selected-items blocks some-fields-restricted?]}]
  (let [item-count (count selected-items)
        [t] (useTranslation)

        fetcher (useFetcher)
        prev-state (uix/use-ref nil)

        on-submit (fn [data]
                    (let [update-data (jc (.-update data))
                          item-ids (vec (map :id selected-items))]
                      (.submit fetcher
                               (js/JSON.stringify (cj {:selected-items item-ids
                                                       :update update-data}))
                               (cj {:method "PATCH"
                                    :encType "application/json"}))))

        on-invalid (fn [errors]
                     (js/console.warn "Edit dialog validation failed:" (clj->js errors)))]

    (uix/use-effect
     (fn []
       (when (and (= @prev-state "loading")
                  (= (.-state fetcher) "idle"))
         (.. toast (success (t "pool.models.search_edit.dialog.success" #js {:count item-count})))
         (on-open-change false))
       (reset! prev-state (.-state fetcher)))
     [fetcher on-open-change t item-count])

    ($ Dialog {:open open?
               :onOpenChange on-open-change
               :modal false}

       (when open? ($ :div {:class-name "fixed top-0 left-0 inset-0 z-50 bg-black/80 w-screen h-screen transition-opacity"
                            :data-dialog-overlay true}))

       ($ DialogContent {:class-name "max-w-[768px] lg:max-w-[1024px]"
                         :onInteractOutside (fn [e]
                                              (let [target (.. e -target)]
                                                (when-not (.closest target "[data-dialog-overlay]")
                                                  (.preventDefault e))))}

          ($ DialogHeader
             ($ DialogTitle
                (t "pool.models.search_edit.dialog.title" #js {:count item-count}))
             ($ DialogDescription
                (t "pool.models.search_edit.dialog.description")))

          ($ PatchItemForm {:blocks blocks
                            :on-submit on-submit
                            :on-invalid on-invalid})

          ($ Alert {:class-name "border-amber-200 bg-amber-50 text-amber-900 dark:border-amber-900 dark:bg-amber-950 dark:text-amber-50"}
             ($ AlertDescription ($ :span {:class-name "flex items-start"}
                                    ($ Info {:class-name "w-5 h-5 mr-2 inline"})
                                    ($ :span (t "pool.models.search_edit.dialog.warning_description" #js {:count item-count})))))

          (when some-fields-restricted?
            ($ Alert {:class-name "border-amber-200 bg-amber-50 text-amber-900 dark:border-amber-900 dark:bg-amber-950 dark:text-amber-50"}
               ($ AlertDescription ($ :span {:class-name "flex items-start"}
                                      ($ Info {:class-name "w-5 h-5 mr-2 inline"})
                                      ($ :span (t "pool.models.search_edit.dialog.restricted_fields_warning"))))))

          ($ DialogFooter {:class-name "pt-4"}
             ($ Button {:type "button"
                        :variant "outline"
                        :on-click #(on-open-change false)
                        :disabled (or (= (.-state fetcher) "submitting")
                                      (= (.-state fetcher) "loading"))}
                (t "pool.models.search_edit.dialog.cancel"))
             ($ Button {:data-test-id "apply-button"
                        :type "submit"
                        :form "edit-dialog-form"
                        :class-name "disabled:hover:bg-primary/50"
                        :disabled (or (zero? item-count)
                                      (= (.-state fetcher) "submitting")
                                      (= (.-state fetcher) "loading"))}

                (if (or (= (.-state fetcher) "submitting")
                        (= (.-state fetcher) "loading"))
                  (t "pool.models.search_edit.dialog.submitting")
                  (t "pool.models.search_edit.dialog.submit"))

                (when (or (= (.-state fetcher) "submitting")
                          (= (.-state fetcher) "loading"))
                  ($ Spinner {:class-name "w-5 h-5 ml-2"}))))))))
