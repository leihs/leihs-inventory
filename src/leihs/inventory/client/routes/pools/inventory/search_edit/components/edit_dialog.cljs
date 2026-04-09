(ns leihs.inventory.client.routes.pools.inventory.search-edit.components.edit-dialog
  (:require
   ["@@/button" :refer [Button]]
   ["@@/dialog" :refer [Dialog DialogContent DialogDescription DialogFooter
                        DialogHeader DialogTitle]]
   ["@@/spinner" :refer [Spinner]]
   ["react-router-dom" :refer [useFetcher]]
   [leihs.inventory.client.components.patch-item-form :refer [PatchItemForm]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [$ defui]]))

;; Main EditDialog Component
(defui EditDialog [{:keys [open? on-open-change selected-items blocks]}]
  (let [item-count (count selected-items)

        fetcher (useFetcher)

        on-submit (fn [data]
                    (let [update-data (jc (.-update data))]
                      (.submit fetcher
                               (js/JSON.stringify (cj {:selected-items (vec selected-items)
                                                       :update update-data}))
                               (cj {:method "PATCH"
                                    :encType "application/json"}))))

        on-invalid (fn [errors]
                     (js/console.warn "Edit dialog validation failed:" (clj->js errors)))]

    (uix/use-effect
     (fn []
       (when (= (.-state fetcher) "idle")
         (on-open-change false)))
     [fetcher on-open-change])

    ($ Dialog {:open open?
               :onOpenChange on-open-change
               :modal false}

       (when open? ($ :div {:class-name "fixed top-0 left-0 inset-0 z-50 bg-black/80 w-screen h-screen transition-opacity"}))

       ($ DialogContent {:class-name "max-w-[768px] lg:max-w-[1024px]"}
          ($ DialogHeader
             ($ DialogTitle
                (str "Edit " item-count " " (if (= item-count 1) "item" "items")))
             ($ DialogDescription
                "Select the fields you want to update for all selected items."))

          ($ PatchItemForm {:blocks blocks
                            :on-submit on-submit
                            :on-invalid on-invalid})

          ($ DialogFooter {:class-name "pt-4"}
             ($ Button {:type "button"
                        :variant "outline"
                        :on-click #(on-open-change false)
                        :disabled (or (= (.-state fetcher) "submitting")
                                      (= (.-state fetcher) "loading"))}
                "Abbrechen")
             ($ Button {:type "submit"
                        :form "edit-dialog-form"
                        :class-name "disabled:hover:bg-primary/50"
                        :disabled (or (zero? item-count)
                                      (= (.-state fetcher) "submitting")
                                      (= (.-state fetcher) "loading"))}

                (if (or (= (.-state fetcher) "submitting")
                        (= (.-state fetcher) "loading"))
                  ($ :span {:class-name "flex items-center"}
                     "Wird angewendet"
                     ($ Spinner {:class-name "w-5 h-5 ml-2"}))

                  (str "Auf " item-count " "
                       (if (= item-count 1) "Gegenstand" "Gegenstände")
                       " anwenden"))))))))
