(ns leihs.inventory.client.routes.pools.inventory.scan-edit.page
  (:require
   ["@@/button" :refer [Button]]
   ["@@/card" :refer [Card CardContent CardHeader CardTitle]]
   ["@@/form" :refer [Form FormControl FormField FormItem]]
   ["@@/input-group" :refer [InputGroup InputGroupAddon InputGroupInput]]
   ["@@/spinner" :refer [Spinner]]
   ["lucide-react" :refer [Barcode SquarePen]]
   ["react-hook-form" :refer [useForm]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :refer [useFetcher useLoaderData]]
   ["sonner" :refer [toast]]
   [clojure.string :as str]
   [leihs.inventory.client.components.form.fields.autocomplete-field :refer [AutocompleteField]]
   [leihs.inventory.client.components.patch-item-form :refer [PatchItemForm]]
   [leihs.inventory.client.components.typo :refer [Typo]]
   [leihs.inventory.client.lib.dynamic-form :as dynamic-form]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [promesa.core :as p]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui page []
  (let [[t] (useTranslation)
        {:keys [data]} (useLoaderData)
        form (useForm #js {:defaultValues #js {:item nil
                                               :inventory_code ""}})
        button-ref (uix/use-ref nil)
        barcode-input-ref (uix/use-ref nil)
        submitted-via-scan? (uix/use-ref false)
        fetcher (useFetcher)
        prev-state (uix/use-ref nil)
        get-values (aget form "getValues")
        reset (aget form "reset")
        resetField (aget form "resetField")
        fields (:fields data)
        structure (-> (dynamic-form/fields->structure fields)
                      (dynamic-form/patch "price" {:component "price-input"}))
        blocks (uix/use-memo
                (fn []
                  (->> structure
                       (mapcat :blocks)
                       (map #(if (and (:label %) (not (str/starts-with? (:name %) "properties_")))
                               (update % :label t)
                               %))
                       (sort #(.localeCompare (:label %1) (:label %2)))
                       (remove #(#{"attachments"} (:name %)))
                       vec))
                [structure t])

        on-submit (uix/use-callback
                   (fn [data]
                     (p/let [update-data (jc data)
                             item (get-values "item")
                             inventory_code (get-values "inventory_code")
                             id (when item (.-value item))
                             payload (cond
                                       (str/blank? inventory_code)
                                       (merge {:id id}
                                              update-data)

                                       (nil? id)
                                       (merge {:inventory_code inventory_code}
                                              update-data))]

                       (reset! submitted-via-scan? (not (str/blank? inventory_code)))
                       (.submit fetcher
                                (js/JSON.stringify (cj payload))
                                (cj {:method "PATCH"
                                     :encType "application/json"}))))
                   [fetcher get-values])

        on-invalid (uix/use-callback
                    (fn [errors]
                      (js/console.warn "Form validation failed with errors:" (cj errors)))
                    [])]

    (uix/use-effect
     (fn []
       (let [data (.-data fetcher)
             state (.-state fetcher)]

         (when (and (= @prev-state "loading")
                    (= state "idle"))
           (if (= (aget data "status") "error")
             (if (= (aget data "message") "invalid inventory code")
               (.. toast (error (t "pool.scan_edit.invalid_item.error")
                                (cj {:description (t "pool.scan_edit.invalid_item.description")
                                     :duration "20000"})))

               (.. toast (error (t "error.action.error")
                                (cj {:description (t "error.action.error_detail"
                                                     #js {:httpStatus (aget data "httpStatus")})}))))

             (do
               (when @submitted-via-scan?
                 (.. @barcode-input-ref (focus)))
               (reset)
               (.. toast (success (t "pool.scan_edit.success"))))))
         (reset! prev-state state)))
     [fetcher t reset])

    (let [state (.-state fetcher)
          submitting? (or (= state "submitting")
                          (= state "loading"))]
      ($ Card {:class-name "my-4"}
         ($ CardHeader
            ($ CardTitle (t "pool.scan_edit.title")))
         ($ CardContent {:class-name "grid grid-cols-2 gap-12"}
            ($ PatchItemForm {:blocks blocks
                              :class-name "h-fit"
                              :on-submit on-submit
                              :on-invalid on-invalid})

            ($ Form (merge form)
               ($ :form {:id "item-form"
                         :className " space-y-2 w-full"
                         :no-validate true}
                  ($ FormField {:control (.-control form)
                                :name "inventory_code"
                                :render #($ FormItem
                                            (let [field (:field (jc %))]
                                              ($ FormControl
                                                 ($ InputGroup
                                                    ($ InputGroupInput
                                                       (merge {:data-barcode-scanner-target true
                                                               :ref barcode-input-ref
                                                               :auto-complete "off"
                                                               :placeholder (t "pool.scan_edit.barcode_placeholder")
                                                               :disabled submitting?
                                                               :name (:name field)
                                                               :on-focus (fn [] (resetField "item"))
                                                               :on-change (:onChange field)
                                                               :on-blur (:onBlur field)
                                                               :on-key-down (fn [e]
                                                                              (when (= (.-key e) "Enter")
                                                                                (.preventDefault e)
                                                                                (.. @button-ref (click))))
                                                               :value (:value field)}))
                                                    ($ InputGroupAddon
                                                       ($ Barcode {:class-name "w-5 h-5"}))

                                                    ($ InputGroupAddon {:align "inline-end"}
                                                       (when (and
                                                              (seq (get-values "inventory_code"))
                                                              submitting?)
                                                         ($ Spinner {:class-name "w-5 h-5"})))))))})

                  ($ Button {:type "submit"
                             :ref button-ref
                             :form "patch-item-form"
                             :data-barcode-scanner-submit-button true
                             :class-name "hidden"})

                  ($ Typo {:variant "description"
                           :class-name "!my-6"}
                     (t "pool.scan_edit.description"))

                  ($ :div {:class-name "flex gap-4 mt-12"}
                     ($ AutocompleteField {:form form
                                           :name "item"
                                           :class-name "mt-0 flex-1"
                                           :props {:values-url "/inventory/:pool-id/items/?owned=true&only_items=true&search_term="
                                                   :text {:select "pool.scan_edit.autocomplete.select"
                                                          :search "pool.scan_edit.autocomplete.search"
                                                          :empty "pool.scan_edit.autocomplete.empty"}
                                                   :remap (fn [item]
                                                            {:value (:id item)
                                                             :label (str (:inventory_code item) " - " (:model_name item))})
                                                   :disabled submitting?
                                                   :on-focus #(resetField "inventory_code")
                                                   :instant true
                                                   :interpolate true}})

                     ($ Button {:type "submit"
                                :form "patch-item-form"
                                :disabled (or (not (get-values "item"))
                                              submitting?)
                                :class-name "self-start disabled:hover:bg-primary"}
                        (if (and (get-values "item") submitting?)
                          ($ Spinner {:class-name "w-5 h-5"})
                          ($ SquarePen {:class-name "w-5 h-5"}))
                        (t "pool.scan_edit.apply_button"))))))))))


