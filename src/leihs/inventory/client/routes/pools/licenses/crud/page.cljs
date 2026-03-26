(ns leihs.inventory.client.routes.pools.licenses.crud.page
  (:require
   ["@/components/react/scrollspy/scrollspy" :refer [Scrollspy ScrollspyItem
                                                     ScrollspyMenu]]
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
   [cljs.core.async :as async :refer [go]]
   [cljs.core.async.interop :refer-macros [<p!]]
    [leihs.inventory.client.provider.visibility-provider :refer [VisibilityProvider]]
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.dynamic-form :as dynamic-form]
   [leihs.inventory.client.lib.dynamic-validation :as dynamic-validation]
   [leihs.inventory.client.lib.form-helper :as form-helper]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.pools.licenses.crud.components.field-dispatcher :refer [FieldDispatcher]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(def groups ["Mandatory data"
             "Status"
             "Invoice Information"
             "General Information"
             "Inventory"
             "Maintenance"])

(defui page []
  (let [[t] (useTranslation)
        location (router/useLocation)
        navigate (router/useNavigate)
        params (router/useParams)

        state (.. location -state)
        is-create (.. location -pathname (includes "create"))
        is-delete (.. location -pathname (includes "delete"))
        is-edit (not (or is-create is-delete))

        pool-id (aget params "pool-id")

        {:keys [data model]} (jc (useLoaderData))

        ;; Use fields directly from API (no custom fields)
        fields (:fields data)

        ;; Extract default values from fields
        defaults (dynamic-form/fields->defaults fields)

        form (useForm (cj {:resolver (zodResolver (dynamic-validation/fields->schema fields))
                           :defaultValues (if is-create
                                            (cj defaults)
                                            (fn [] (form-helper/process-files defaults :attachments)))}))

        get-field-state (.. form -getFieldState)

        set-value (.. form -setValue)

        is-loading (.. form -formState -isLoading)

        control (.. form -control)

        field-building (useWatch (cj {:control control
                                      :name "building_id.value"}))

        building-is-dirty? (:isDirty (jc (get-field-state "building_id")))

        ;; Transform fields data to derived form structure
        structure (uix/use-memo
                   (fn []
                     ;; (js/console.debug "restructure: " fields)
                     (cond-> (dynamic-form/fields->structure fields {:group-order groups})

                       (not is-loading)
                       (dynamic-form/patch "properties_maintenance_currency"
                                           {:props {:bypass-i18n true}})

                       (not is-loading)
                       (dynamic-form/patch "properties_activation_type"
                                           {:props {:bypass-i18n true}})

                       (not is-loading)
                       (dynamic-form/patch "properties_license_type"
                                           {:props {:bypass-i18n true}})

                        ;; Disable software_model_id when set via path param
                       (and is-create model (not is-loading))
                       (dynamic-form/patch "software_model_id"
                                           {:props {:disabled true}
                                            :disabled-reason :model-selected})

                       ;; Disable owner_id on create
                       (and is-create (not is-loading))
                       (dynamic-form/patch "owner_id"
                                           {:props {:disabled true}
                                            :disabled-reason :owner-locked})))
                   [fields is-create model is-loading])

        on-invalid (fn [data]
                     (let [invalid-fields-count (count (jc data))]
                       (.. toast (error (if is-create
                                          (t "pool.licenses.license.create.invalid"
                                             #js {:count invalid-fields-count})
                                          (t "pool.licenses.license.edit.invalid"
                                             #js {:count invalid-fields-count}))))

                       (js/console.debug "is invalid: " data)))

        handle-submit (.. form -handleSubmit)
        on-submit (fn [submit-data event]
                    (go
                      (let [attachments (if is-create
                                          (:attachments (jc submit-data))
                                          (filter (fn [el] (= (:id el) nil))
                                                  (:attachments (jc submit-data))))

                            attachments-to-delete (if is-edit
                                                    (->> (:attachments defaults)
                                                         (map :id)
                                                         (remove (set (map :id (:attachments (jc submit-data))))))
                                                    nil)

                            ;; Add type: "license" to the item data
                            item-data (-> submit-data
                                          jc
                                          (assoc :type "license")
                                          (dissoc :attachments)
                                          (into {}))

                            license-id (aget params "license-id")

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
                                                (.patch (str "/inventory/" pool-id "/items/" license-id)
                                                        (js/JSON.stringify (cj item-data))
                                                        (cj {:cache
                                                             {:update {(keyword license-id) "delete"}}}))

                                                (.then (fn [res]
                                                         {:status (.. res -status)
                                                          :statusText (.. res -statusText)
                                                          :id (.. res -data -id)}))
                                                (.catch (fn [err]
                                                          {:status (.. err -response -status)
                                                           :data (jc (.. err -response -data))
                                                           :statusText (.. err -response -statusText)})))))

                            item-id (:id item-res)]

                        (.. event (preventDefault))

                        (when attachments-to-delete
                          (doseq [attachment-id attachments-to-delete]
                            ;; delete attachments that are not in the new model
                            (<p! (-> http-client
                                     (.delete (str "/inventory/" pool-id "/items/" item-id "/attachments/" attachment-id))
                                     (.then #(.-data %))))))

                        (case (:status item-res)
                          409 (.. toast
                                  (error (if is-create
                                           (t "pool.licenses.license.create.conflict")
                                           (t "pool.licenses.license.edit.conflict"))
                                         (cj {:duration 20000
                                              :action
                                              {:label "Update"
                                               :onClick (fn []
                                                          (set-value "inventory_code" (:proposed_code (:data item-res))))}})))

                          500 (.. toast (error (if is-create
                                                 (t "pool.licenses.license.create.error")
                                                 (t "pool.licenses.license.edit.error"))))

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
                                                     (t "pool.licenses.license.create.success")
                                                     (t "pool.licenses.license.edit.success"))))

                                ;; Navigate back to list
                                (navigate (str "/inventory/" pool-id "/list"
                                               (some-> state .-searchParams))
                                          #js {:state state
                                               :viewTransition true}))

                          ;; default
                          (.. toast (error (:statusText item-res)))))))]

    ;; Handle software_model_id disabling when model is pre-selected
    (uix/use-effect
     (fn []
       (when (and is-create model (not is-loading))
         (set-value "software_model_id" (cj {:label (:product model)
                                             :value (:id model)}))))
     [is-create is-loading model set-value])

    ;; Clear room_id when building changes
    (uix/use-effect
     (fn []
       (when (and field-building (not is-loading) building-is-dirty?)
         (set-value "room_id" nil)))
     [field-building is-loading set-value building-is-dirty?])

    (if is-loading
      ($ :div {:className "flex justify-center items-center h-screen"}
         ($ Spinner))

      ($ :article
         ($ :h1 {:className "text-2xl bold font-bold mt-12 mb-2"}
            (if is-create
              (t "pool.licenses.license.create.title")
              (t "pool.licenses.license.edit.title")))

         ($ :h3 {:className "text-sm mb-6 text-gray-500"}
            (if is-create
              (t "pool.licenses.license.create.description")
              (t "pool.licenses.license.edit.description")))

         ($ Card {:className "py-8 mb-12"}
            ($ CardContent
               ($ Scrollspy {:className "flex gap-4"}
                  ($ ScrollspyMenu {:class-name "w-full lg:w-1/5"})

                  ($ :div {:className "w-full lg:w-4/5"}
                     ($ Form (merge form)
                        ($ VisibilityProvider {:form form}
                           ($ :form {:id "license-form"
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
                                :form "license-form"}
                        (if is-create
                          (t "pool.licenses.license.create.submit")
                          (t "pool.licenses.license.edit.submit")))

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
                                      (t "pool.licenses.license.create.cancel")
                                      (t "pool.licenses.license.edit.cancel")))))))))))))))
