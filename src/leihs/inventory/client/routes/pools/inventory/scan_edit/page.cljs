(ns leihs.inventory.client.routes.pools.inventory.scan-edit.page
  (:require
   ["@@/alert" :refer [Alert AlertDescription]]
   ["@@/button" :refer [Button]]
   ["@@/card" :refer [Card CardContent CardHeader CardTitle]]
   ["@@/form" :refer [Form FormControl FormField FormItem]]
   ["@@/input-group" :refer [InputGroup InputGroupAddon InputGroupInput]]
   ["@@/spinner" :refer [Spinner]]
   ["lucide-react" :refer [Barcode SquarePen Info]]
   ["react-hook-form" :refer [useForm]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router" :refer [Link useParams useFetcher useLoaderData useSearchParams]]
   ["sonner" :refer [toast]]
   [cljs.reader :as reader]
   [clojure.string :as str]
   [leihs.inventory.client.components.export :refer [Export]]
   [leihs.inventory.client.components.form.fields.autocomplete-field :refer [AutocompleteField]]
   [leihs.inventory.client.components.items-table :refer [ItemsTable]]
   [leihs.inventory.client.components.patch-item-form :refer [PatchItemForm]]
   [leihs.inventory.client.components.typo :refer [Typo]]
   [leihs.inventory.client.lib.dynamic-form :as dynamic-form]
   [leihs.inventory.client.lib.hooks :refer [use-barcode-scanner]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [promesa.core :as p]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(def ^:private max-history 150)

(defn- toast-history-full [t on-continue]
  (.. toast (error (t "pool.scan_edit.max_history_warning.title")
                   (cj {:description (t "pool.scan_edit.max_history_warning.description"
                                        #js {:count max-history})
                        :duration "10000"
                        :action (cj {:label (t "pool.scan_edit.max_history_warning.action")
                                     :onClick on-continue})}))))

(defn- toast-fields-changed [t on-continue]
  (.. toast (warning (t "pool.scan_edit.field_change_warning.title")
                     (cj {:description (t "pool.scan_edit.field_change_warning.description")
                          :duration "10000"
                          :action (cj {:label (t "pool.scan_edit.field_change_warning.action")
                                       :onClick on-continue})}))))

(defn- toast-invalid-fields [t]
  (.. toast (error (t "pool.scan_edit.invalid_fields.error")
                   (cj {:description (t "pool.scan_edit.invalid_fields.description")}))))

(defn- toast-invalid-item [t]
  (.. toast (error (t "pool.scan_edit.invalid_item.error")
                   (cj {:description (t "pool.scan_edit.invalid_item.description")
                        :duration "10000"}))))

(defn- toast-action-error [t http-status]
  (.. toast (error (t "error.action.error")
                   (cj {:description (t "error.action.error_detail"
                                        #js {:httpStatus http-status})}))))

(defn- toast-success [t]
  (.. toast (success (t "pool.scan_edit.success"))))

(defn- toast-owner-only [t fields]
  (.. toast (error (t "pool.scan_edit.owner_only.error")
                   (cj {:duration "10000"
                        :description ($ :span
                                        ($ :p (t "pool.scan_edit.owner_only.description" #js {:count (count fields)}))
                                        ($ :ul {:class-name "mt-2 list-disc ml-3"}
                                           (for [f fields]
                                             ($ :li {:key f} f))))}))))

(defui page []
  (let [[t] (useTranslation)
        [scanned-code clear!] (use-barcode-scanner)
        [restricted-labels set-restricted-labels] (uix/use-state [])
        {:keys [data items]} (useLoaderData)
        [search-params set-search-params!] (useSearchParams)
        {:keys [pool-id]} (jc (useParams))
        form (useForm #js {:defaultValues #js {:item nil
                                               :inventory_code ""}})
        button-ref (uix/use-ref nil)
        fetcher (useFetcher)
        prev-state (uix/use-ref nil)
        last-submit-field-names-ref (uix/use-ref nil)
        current-field-names-ref (uix/use-ref nil)

        get-values (aget form "getValues")
        reset (aget form "reset")
        resetField (aget form "resetField")
        setValue (aget form "setValue")

        fields (:fields data)
        initial-fields (->> (.getAll search-params "field")
                            (map reader/read-string)
                            vec)
        _ (reset! current-field-names-ref (mapv :name initial-fields))

        export-url (fn []
                     (let [base-url (str "/inventory/" pool-id "/items/")]
                       (str base-url "?" (->> (.getAll search-params "ids")
                                              (map #(str "ids=" %))
                                              (str/join "&")))))

        structure (-> (dynamic-form/fields->structure fields)
                      (dynamic-form/patch "price" {:component "price"}))

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

        handle-fields-change (fn [fields]
                               (let [owner-only-names (->> fields
                                                           (filter (fn [f] (true? (:owner_only f))))
                                                           (map :name)
                                                           set)
                                     labels (->> blocks
                                                 (filter (fn [block] (owner-only-names (:name block))))
                                                 (map :label))]

                                 (set-restricted-labels labels)
                                 (reset! current-field-names-ref (vec (keep :name fields)))
                                 (set-search-params! (fn [params]
                                                       (.delete params "field")
                                                       (doseq [f fields]
                                                         (when (:name f)
                                                           (.append params "field" (pr-str {:name (:name f) :value (:value f)}))))
                                                       params))))

        on-submit (uix/use-callback
                   (fn [data]
                     (let [submit! (fn []
                                     (p/let [update-data (jc data)
                                             item (get-values "item")
                                             inventory_code (get-values "inventory_code")
                                             id (when item (.-value item))
                                             payload (cond
                                                       (str/blank? inventory_code)
                                                       (merge {:id id} update-data)
                                                       (nil? id)
                                                       (merge {:inventory_code inventory_code} update-data))]
                                       (clear!)
                                       (.submit fetcher
                                                (js/JSON.stringify (cj payload))
                                                (cj {:method "PATCH"
                                                     :encType "application/json"}))))
                           reset-and-submit! (fn []
                                               (set-search-params! (fn [p] (.delete p "ids") p))
                                               (submit!))

                           history-count (count (.getAll search-params "ids"))
                           fields-changed? (and (pos? history-count)
                                                (some? @last-submit-field-names-ref)
                                                (not= @current-field-names-ref @last-submit-field-names-ref))]

                       (cond
                         (>= history-count max-history)
                         (toast-history-full t reset-and-submit!)

                         fields-changed?
                         (toast-fields-changed t reset-and-submit!)

                         :else
                         (submit!))))
                   [fetcher get-values clear! t search-params set-search-params!])

        on-invalid (uix/use-callback
                    (fn [errors]
                      (toast-invalid-fields t)
                      (clear!)
                      (reset)
                      (js/console.warn "Form validation failed with errors:" (cj errors)))
                    [t clear! reset])]

    (uix/use-effect
     (fn []
       (when scanned-code
         (resetField "item")
         (setValue "inventory_code" scanned-code)
         (.. @button-ref (click))))
     [scanned-code setValue resetField])

    (uix/use-effect
     (fn []
       (let [data (.-data fetcher)
             state (.-state fetcher)]

         (when (and (contains? #{"loading"
                                 "submitting"} @prev-state)
                    (= state "idle"))

           (cond
             (= (aget data "message") "invalid inventory code")
             (toast-invalid-item t)

             (and (= (aget data "status") "error")
                  (= (aget data "httpStatus") 400)
                  (= (aget data "response" "error") "Unpermitted fields"))
             (toast-owner-only t restricted-labels)

             (= (aget data "status") "error")
             (toast-action-error t (aget data "httpStatus"))

             :else
             (do
               (reset! last-submit-field-names-ref @current-field-names-ref)
               (reset)
               (when-let [id (aget data "id")]
                 (set-search-params! (fn [params]
                                       (when-not (.has params "ids" id)
                                         (.append params "ids" id))
                                       params)))
               (toast-success t))))
         (reset! prev-state state)))
     [fetcher t reset set-search-params! blocks restricted-labels])

    (let [state (.-state fetcher)
          submitting? (or (= state "submitting")
                          (= state "loading"))]

      ($ :<>
         ($ Card {:class-name "my-4"}
            ($ CardHeader
               ($ CardTitle (t "pool.scan_edit.title")))
            ($ CardContent {:class-name "flex flex-col lg:grid lg:grid-rows-1 lg:grid-cols-2 gap-12"}
               ($ :div {:class-name "space-y-2"}
                  ($ PatchItemForm {:blocks blocks
                                    :class-name "h-fit"
                                    :initial-fields initial-fields
                                    :on-fields-change handle-fields-change
                                    :on-submit on-submit
                                    :on-invalid on-invalid})

                  (when (> (count restricted-labels) 0)
                    ($ Alert {:class-name "border-amber-200 bg-amber-50 text-amber-900 dark:border-amber-900 dark:bg-amber-950 dark:text-amber-50"}
                       ($ AlertDescription ($ :span {:class-name "flex items-center space-x-2"}
                                              ($ Info {:class-name "w-5 h-5 flex-none"})
                                              ($ :span (t "pool.scan_edit.owner_only_warning")))))))

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
                                                          (merge {:auto-complete "off"
                                                                  :data-test-id "barcode-input"
                                                                  :data-barcode "true"
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
                                :class-name "hidden"})

                     ($ Typo {:variant "description"
                              :class-name "!my-6"}
                        (t "pool.scan_edit.description"))

                     ($ :div {:class-name "flex gap-4 mt-12"}
                        ($ AutocompleteField {:form form
                                              :name "item"
                                              :class-name "mt-0 min-w-0 flex-1"
                                              :props {:values-url "/inventory/:pool-id/items/?search_term="
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
                                   :class-name "self-start shrink-0 disabled:hover:bg-primary"}
                           (if (and (get-values "item") submitting?)
                             ($ Spinner {:class-name "w-5 h-5"})
                             ($ SquarePen {:class-name "w-5 h-5"}))
                           (t "pool.scan_edit.apply_button")))))))

         (when (seq items)
           ($ Card {:class-name "my-4"}
              ($ CardHeader {:class-name "flex flex-row items-center justify-between"}
                 ($ CardTitle (t "pool.scan_edit.history.title"))

                  ;; Bulk action buttons - show when items selected
                 ($ Export {:url (export-url)
                            :count (count (.getAll search-params "ids"))}))
              ($ CardContent
                 ($ ItemsTable {:items items
                                :row-action (fn [item]
                                              ($ Button {:variant "outline"
                                                         :asChild true}
                                                 ($ Link {:to (str "../items/" (:id item))
                                                          :viewTransition true}
                                                    (t "pool.models.list.actions.edit"))))}))))))))
