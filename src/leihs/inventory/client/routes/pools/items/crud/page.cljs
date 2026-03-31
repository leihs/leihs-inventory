(ns leihs.inventory.client.routes.pools.items.crud.page
  (:require
   ["@/components/react/scrollspy/scrollspy" :refer [Scrollspy ScrollspyItem
                                                     ScrollspyMenu]]
   ["@@/alert" :refer [Alert AlertDescription AlertTitle]]
   ["@@/button" :refer [Button]]
   ["@@/button-group" :refer [ButtonGroup ButtonGroupSeparator]]
   ["@@/card" :refer [Card CardContent]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent
                               DropdownMenuGroup DropdownMenuItem
                               DropdownMenuTrigger]]
   ["@@/form" :refer [Form]]
   ["@@/spinner" :refer [Spinner]]
   ["@hookform/resolvers/zod" :refer [zodResolver]]
   ["lucide-react" :refer [ChevronDownIcon]]
   ["react-hook-form" :refer [useForm useWatch]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [Link useLoaderData]]
   ["sonner" :refer [toast]]
   ["zod" :as z]
   [cljs.core.async :as async :refer [go]]
   [cljs.core.async.interop :refer-macros [<p!]]
   [clojure.string :as str]
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.dynamic-form :as dynamic-form]
   [leihs.inventory.client.lib.dynamic-validation :as dynamic-validation]
   [leihs.inventory.client.lib.form-helper :as form-helper]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.provider.visibility-provider :refer [VisibilityProvider]]
   [leihs.inventory.client.routes.pools.items.crud.components.field-dispatcher :refer [FieldDispatcher]]
   [leihs.inventory.client.routes.pools.items.crud.config :as config]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

;; Group definitions per entity type
(def groups
  {:item ["Mandatory data" "Status" "Inventory" "Eigenschaften"
          "General Information" "Location" "Invoice Information"]
   :package ["Package" "Content" "Status" "Inventory"
             "General Information" "Location" "Invoice Information"]
   :license ["Mandatory data" "Status" "Invoice Information"
             "General Information" "Inventory" "Maintenance"]})

(defui page []
  (let [[t] (useTranslation)
        location (router/useLocation)
        navigate (router/useNavigate)
        params (router/useParams)
        [search-params _] (router/useSearchParams)

        ;; Determine entity type from route params
        item-type (cond
                    (str/includes? (.-pathname location) "licenses")
                    :license
                    (str/includes? (.-pathname location) "packages")
                    :package
                    :else
                    :item)

        config (item-type config/types)
        t-ns (:translation-namespace config)

        state (.. location -state)
        is-create (.. location -pathname (includes "create"))
        is-delete (.. location -pathname (includes "delete"))
        is-edit (not (or is-create is-delete))
        is-copy (.has search-params "fromItem")

        pool-id (aget params "pool-id")

        {:keys [data copy-data model package package-model items]} (jc (useLoaderData))

        ;; Get entity-specific groups
        groups (get groups item-type)

        ;; Define custom fields based on entity type
        custom-fields (cond
                        ;; Items: batch creation count field
                        (and (= item-type :item) is-create)
                        [{:id "count"
                          :component "input"
                          :group "Mandatory data"
                          :position 0
                          :required true
                          :default 1
                          :props {:type "number"
                                  :min 0
                                  :max 999999
                                  :step 1}
                          :validator (-> (.. z -coerce (number))
                                         (.min 0)
                                         (.max 999999)
                                         (.int))}]

                        ;; Packages: item selection field
                        (= item-type :package)
                        [{:id "item_ids"
                          :type "array"
                          :component "items"
                          :group "Content"
                          :required true
                          :default (or items [])
                          :props {:text {:select (str t-ns ".fields.items.select")
                                         :search (str t-ns ".fields.items.search")
                                         :searching (str t-ns ".fields.items.searching")
                                         :search_empty (str t-ns ".fields.items.search_empty")
                                         :not_found (str t-ns ".fields.items.not_found")}}
                          :validator (if is-create
                                       (-> (z/array (z/object (cj {:id (z/guid)})))
                                           (.min 1)
                                           (.transform (fn [arr] (mapv (fn [item] (.-id item)) arr))))
                                       (-> (z/array (z/object (cj {:id (z/guid)})))
                                           (.transform (fn [arr] (mapv (fn [item] (.-id item)) arr)))))}]

                        ;; Licenses: no custom fields
                        :else nil)

        ;; Merge API fields with custom fields
        fields (concat (:fields data) custom-fields)

        ;; Extract default values from fields
        defaults (dynamic-form/fields->defaults fields)
        copy-defaults (when is-copy (dynamic-form/fields->defaults (:fields copy-data)))

        form (useForm (cj {:resolver (zodResolver (dynamic-validation/fields->schema fields))
                           :defaultValues (cond
                                            is-copy
                                            (merge defaults
                                                   (dissoc copy-defaults :inventory_code :serial_number :attachments))
                                            is-create
                                            (cj defaults)
                                            is-edit
                                            (fn [] (form-helper/process-files defaults :attachments)))}))

        set-value (.. form -setValue)
        get-values (.. form -getValues)
        is-loading (.. form -formState -isLoading)
        control (.. form -control)

        field-building (useWatch (cj {:control control
                                      :name "building_id.value"}))

        ;; Always call hooks unconditionally (React hooks rules)
        field-count (useWatch (cj {:control control
                                   :name "count"}))
        watched-items (useWatch (cj {:control control
                                     :name "item_ids"}))

        building-ref (uix/use-ref field-building)
        prev-items-count-ref (uix/use-ref (or (count watched-items) 0))

        ;; Then conditionally use their values
        batch? (and (= item-type :item) (> (or field-count 1) 1))

        ;; Transform fields data to derived form structure
        structure (uix/use-memo
                   (fn []
                     (cond-> (dynamic-form/fields->structure
                              fields
                              {:group-order groups
                               :main-group (when (= item-type :package) "Package")})

                       ;; Disable model_id/software_model_id when set via path param
                       (and is-create model (not is-loading))
                       (dynamic-form/patch (if (= item-type :license)
                                             "software_model_id"
                                             "model_id")
                                           {:props {:disabled true}
                                            :disabled-reason :model-selected})

                       ;; Items: Disable fields when batch creating (count > 1)
                       (and (= item-type :item) is-create (> (or field-count 1) 1) (not is-loading))
                       (-> (dynamic-form/patch "inventory_code"
                                               {:props {:disabled true}
                                                :disabled-reason :multiple-items})
                           (dynamic-form/patch "attachments"
                                               {:props {:disabled true}
                                                :disabled-reason :multiple-items})
                           (dynamic-form/patch "serial_number"
                                               {:props {:disabled true}
                                                :disabled-reason :multiple-items}))

                       ;; Disable owner_id on create
                       (and is-create (not is-loading))
                       (dynamic-form/patch "owner_id"
                                           {:props {:disabled true}
                                            :disabled-reason :owner-locked})

                       ;; Licenses: bypass i18n for certain fields
                       (and (= item-type :license) (not is-loading))
                       (-> (dynamic-form/patch "properties_maintenance_currency"
                                               {:props {:bypass-i18n true}})
                           (dynamic-form/patch "properties_activation_type"
                                               {:props {:bypass-i18n true}})
                           (dynamic-form/patch "properties_license_type"
                                               {:props {:bypass-i18n true}}))))
                   [fields is-create model is-loading field-count item-type groups])

        on-invalid (fn [data]
                     (let [invalid-fields-count (count (jc data))]
                       (.. toast (error (if is-create
                                          (t (str t-ns ".create.invalid")
                                             #js {:count invalid-fields-count})
                                          (t (str t-ns ".edit.invalid")
                                             #js {:count invalid-fields-count}))))

                       (js/console.debug "is invalid: " data)))

        handle-submit (.. form -handleSubmit)
        on-submit (fn [submit-data event]
                    (go
                      (let [attachments (if is-create
                                          (if batch? nil (:attachments (jc submit-data)))
                                          (filter (fn [el] (= (:id el) nil))
                                                  (:attachments (jc submit-data))))

                            attachments-to-delete (if is-edit
                                                    (->> (:attachments defaults)
                                                         (map :id)
                                                         (remove (set (map :id (:attachments (jc submit-data))))))
                                                    nil)

                            item-data (-> submit-data
                                          jc
                                          (cond-> batch? (dissoc :serial_number :inventory_code))
                                          (dissoc :attachments)
                                          (into {}))

                            item-id (aget params "item-id")

                            item-res (if is-create
                                       (<p! (-> http-client
                                                (.post (str "/inventory/" pool-id "/items/")
                                                       (js/JSON.stringify (cj item-data)))

                                                (.then (fn [res]
                                                         {:status (.. res -status)
                                                          :statusText (.. res -statusText)
                                                          :data (jc (.. res -data))
                                                          :id (.. res -data -id)}))
                                                (.catch (fn [err]
                                                          {:status (.. err -response -status)
                                                           :data (jc (.. err -response -data))
                                                           :statusText (.. err -response -statusText)}))))

                                       (<p! (-> http-client
                                                (.patch (str "/inventory/" pool-id "/items/" item-id)
                                                        (js/JSON.stringify (cj item-data))
                                                        (cj {:cache
                                                             {:update {(keyword item-id) "delete"}}}))

                                                (.then (fn [res]
                                                         {:status (.. res -status)
                                                          :statusText (.. res -statusText)
                                                          :id (.. res -data -id)}))
                                                (.catch (fn [err]
                                                          {:status (.. err -response -status)
                                                           :data (jc (.. err -response -data))
                                                           :statusText (.. err -response -statusText)})))))

                            item-id (when (not= (:status item-res) "200") (:id item-res))]

                        (.. event (preventDefault))

                        (when attachments-to-delete
                          (doseq [attachment-id attachments-to-delete]
                            (<p! (-> http-client
                                     (.delete (str "/inventory/" pool-id "/items/" item-id "/attachments/" attachment-id))
                                     (.then #(.-data %))))))

                        (case (:status item-res)
                          409 (.. toast
                                  (error (if is-create
                                           (t (str t-ns ".create.conflict"))
                                           (t (str t-ns ".edit.conflict")))
                                         (cj {:duration 20000
                                              :action
                                              {:label "Update"
                                               :onClick (fn []
                                                          (set-value "inventory_code" (:proposed_code (:data item-res))))}})))

                          500 (.. toast (error (if is-create
                                                 (t (str t-ns ".create.error"))
                                                 (t (str t-ns ".edit.error")))))

                          200 (do
                                ;; upload attachments sequentially
                                (doseq [attachment attachments]
                                  (let [file (:file attachment)
                                        binary-data (<p! (.. file (arrayBuffer)))
                                        type (.. file -type)
                                        name (.. file -name)]

                                    (<p! (-> http-client
                                             (.post (str "/inventory/" pool-id "/items/" item-id "/attachments/")
                                                    binary-data
                                                    (cj {:headers {"Content-Type" type
                                                                   "X-Filename" name}}))))))

                                (.. toast (success (if is-create
                                                     (t (str t-ns ".create.success"))
                                                     (t (str t-ns ".edit.success")))))

                                ;; state needs to be forwarded for back navigation
                                (cond
                                  ;; Items: batch creation -> review page
                                  (and (= item-type :item) batch?)
                                  (let [get-ids (fn [data] (mapv (fn [item] [:ids (:id item)]) data))
                                        model-id (->> item-res :data first :model_id)
                                        params-search (router/createSearchParams (cj (conj (get-ids (:data item-res))
                                                                                           [:mid model-id])))]

                                    (navigate (str "/inventory/" pool-id "/items/review?" params-search)
                                              #js {:state state
                                                   :viewTransition true}))

                                  ;; Default: back to list
                                  :else
                                  (navigate (str "/inventory/" pool-id "/list"
                                                 (some-> state .-searchParams))
                                            #js {:state state
                                                 :viewTransition true})))

                          ;; default
                          (.. toast (error (:statusText item-res)))))))]

    ;; Handle model_id disabling when model is pre-selected
    (uix/use-effect
     (fn []
       (when (and is-create model
                  (not is-loading))
         (let [field-name (if (= item-type :license) "software_model_id" "model_id")
               label-field (if (= item-type :license) :product :product)]
           (set-value field-name (cj {:label (get model label-field)
                                      :value (:id model)})))))
     [is-create is-loading model set-value item-type])

    ;; Clear room_id when building changes
    (uix/use-effect
     (fn []
       (let [prev-building (.-current building-ref)]
         (when (and field-building
                    (not is-loading)
                    (not= field-building prev-building)
                    prev-building) ; Only clear if there was a previous value
           (set-value "room_id" nil))
         (set! (.-current building-ref) field-building)))
     [field-building is-loading set-value])

    (uix/use-effect
     (fn []
       (when (= item-type :package)
         (let [prev-count @prev-items-count-ref
               curr-count (count watched-items)
               no-retired-reason? (empty? (get-values "retired_reason"))]
         ;; Only trigger when transitioning from non-zero to zero
           (when (and (not is-loading)
                      is-edit
                      (pos? prev-count) ; Was non-zero
                      (zero? curr-count)) ; Now zero
             (set-value "retired" "true")

             (when no-retired-reason?
               (set-value "retired_reason" (t "pool.packages.package.edit.auto_retire_reason")))

             (.. toast (warning (t "pool.packages.package.edit.empty_package"))))
            ;; Update ref for next render
           (reset! prev-items-count-ref curr-count))))
     [watched-items is-loading is-edit set-value get-values t item-type])

    (if is-loading
      ($ :div {:className "flex justify-center items-center h-screen"}
         ($ Spinner))

      ($ :article
         ($ :h1 {:className "text-2xl bold font-bold mt-12 mb-2"}
            (if is-create
              (t (str t-ns ".create.title"))
              (t (str t-ns ".edit.title"))))

         ($ :h3 {:className "text-sm mb-6 text-gray-500"}
            (if is-create
              (t (str t-ns ".create.description"))
              (t (str t-ns ".edit.description"))))

         ($ Card {:className "py-8 mb-12"}
            ($ CardContent
               ($ Scrollspy {:className "flex gap-4"}
                  ($ ScrollspyMenu {:class-name "w-full lg:w-1/5"})

                  ($ :div {:className "w-full lg:w-4/5"}
                      ;; Items: show package membership alert
                     (when (and (= item-type :item) package)
                       ($ Alert {:class-name "mb-6 border-blue-200 bg-blue-50 text-blue-900 dark:border-blue-900 dark:bg-blue-950 dark:text-blue-50"}
                          ($ AlertTitle {:class-name "text-sm font-medium"}
                             (t (str t-ns ".edit.package_item")))
                          ($ AlertDescription {:class-name "text-xs text-muted-foreground"}
                             ($ Link {:to (str "/inventory/" pool-id "/packages/" (:id package))
                                      :viewTransition true}
                                (str (:product package-model) " - " (:inventory_code package))))))

                     ($ Form (merge form)
                        ($ VisibilityProvider {:form form}
                           ($ :form {:id "item-form"
                                     :className "space-y-12 "
                                     :no-validate true
                                     :on-submit (handle-submit on-submit on-invalid)}

                              (for [section structure]
                                ($ ScrollspyItem {:className "scroll-mt-[10vh]"
                                                  :key (:title section)
                                                  :id (:title section)
                                                  :name (t (:title section))}

                                   ($ :h2 {:className "text-lg"} (t (:title section)))
                                   ($ :hr {:className "mb-4"})

                                   (for [block (:blocks section)]
                                     ($ FieldDispatcher {:key (:name block)
                                                         :form form
                                                         :block block}))))))))

                  ($ ButtonGroup {:class-name "ml-auto sticky self-end bottom-[1.5rem]"}
                     ($ Button {:type "submit"
                                :form "item-form"}
                        (if is-create
                          (str (when (and (= item-type :item) batch?)
                                 (str field-count " x "))
                               (t (str t-ns ".create.submit")))
                          (t (str t-ns ".edit.submit"))))

                     ($ ButtonGroupSeparator)
                     ($ DropdownMenu
                        ($ DropdownMenuTrigger {:asChild true}
                           ($ Button {:data-test-id "submit-dropdown"
                                      :className "self-center !px-2"}
                              ($ ChevronDownIcon)))

                        ($ DropdownMenuContent {:align "end"
                                                :class-name "[--radius:1rem]"}
                           ($ DropdownMenuGroup
                              ($ DropdownMenuItem
                                 {:asChild true}
                                 ($ Link {:to (str (router/generatePath "/inventory/:pool-id/list" params)
                                                   (some-> state .-searchParams))
                                          :viewTransition true}
                                    (if is-create
                                      (t (str t-ns ".create.cancel"))
                                      (t (str t-ns ".edit.cancel"))))))))))))))))
