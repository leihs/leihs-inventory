(ns leihs.inventory.client.routes.pools.items.crud.page
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
   ["zod" :as z]
   [cljs.core.async :as async :refer [go]]
   [cljs.core.async.interop :refer-macros [<p!]]
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.dynamic-form :as dynamic-form]
   [leihs.inventory.client.lib.dynamic-validation :as dynamic-validation]
   [leihs.inventory.client.lib.form-helper :as form-helper]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.pools.items.crud.components.fields :as form-fields]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(def groups ["Mandatory data"
             "Status"
             "Inventory"
             "Eigenschaften"
             "General Information"
             "Location"
             "Invoice Information"])

(defui page []
  (let [[t] (useTranslation)
        location (router/useLocation)
        navigate (router/useNavigate)

        state (.. location -state)
        is-create (.. location -pathname (includes "create"))
        is-delete (.. location -pathname (includes "delete"))

        is-edit (not (or is-create is-delete))

        {:keys [data model]} (jc (useLoaderData))

        ;; Define custom fields for create mode only
        custom-fields (if is-create
                        [{:id "count"
                          :type "number"
                          :component "input"
                          :group "Mandatory data"
                          :position 0
                          :label "Item Count"
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
                        nil)

        ;; Merge API fields with custom fields
        fields (concat (:fields data) custom-fields)

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
        params (router/useParams)

        field-building (useWatch (cj {:control control
                                      :name "building_id.value"}))

        building-is-dirty? (:isDirty (jc (get-field-state "building_id")))

        field-count (useWatch (cj {:control control
                                   :name "count"}))
        batch? (> field-count 1)

        ;; Transform fields data to derived form structure
        structure (uix/use-memo
                   (fn []
                     (cond-> (dynamic-form/fields->structure fields {:group-order groups})

                       ;; Disable model_id when set via path param
                       (and is-create model (not is-loading))
                       (dynamic-form/patch "model_id"
                                           {:props {:disabled true}
                                            :disabled-reason :model-selected})

                       ;; Disable fields when batch creating (count > 1)                                                  
                       (and is-create (> field-count 1) (not is-loading))
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
                                            :disabled-reason :owner-locked})))
                   [fields is-create model is-loading field-count])

        on-invalid (fn [data]
                     (let [invalid-fields-count (count (jc data))]
                       (.. toast (error (if is-create
                                          (t "pool.items.item.create.invalid"
                                             #js {:count invalid-fields-count})
                                          (t "pool.items.item.edit.invalid"
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

                            pool-id (aget params "pool-id")

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

                                       (<p! (let [item-id (aget params "item-id")]
                                              (-> http-client
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
                                                             :statusText (.. err -response -statusText)}))))))

                            item-id (when (not= (:status item-res) "200") (:id item-res))]

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
                                           (t "pool.items.item.create.conflict")
                                           (t "pool.items.item.edit.conflict"))
                                         (cj {:duration 20000
                                              :action
                                              {:label "Update"
                                               :onClick (fn []
                                                          (set-value "inventory_code" (:proposed_code (:data item-res))))}})))

                          500 (.. toast (error (if is-create
                                                 (t "pool.items.item.create.error")
                                                 (t "pool.items.item.edit.error"))))

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
                                                     (t "pool.items.item.create.success")
                                                     (t "pool.items.item.edit.success"))))

                                ;; state needs to be forwarded for back navigation
                                (cond
                                  batch?
                                  (let [get-ids (fn [data] (mapv (fn [item] [:ids (:id item)]) data))
                                        model-id (->> item-res :data first :model_id)
                                        params (router/createSearchParams (cj (conj (get-ids (:data item-res))
                                                                                    [:mid model-id])))]

                                    (navigate (str "/inventory/" pool-id "/items/review?" params)
                                              #js {:state state
                                                   :viewTransition true}))
                                  is-create
                                  (navigate (str "/inventory/" pool-id "/list"
                                                 (some-> state .-searchParams))
                                            #js {:state state
                                                 :viewTransition true})

                                  is-edit
                                  (navigate (str "/inventory/" pool-id "/list"
                                                 (some-> state .-searchParams))
                                            #js {:state state
                                                 :viewTransition true})))

                          ;; default
                          (.. toast (error (:statusText item-res)))))))]

    ;; Handle model_id disabling when model is pre-selected
    (uix/use-effect
     (fn []
       (when (and is-create model (not is-loading))
         (set-value "model_id" (cj {:label (:product model)
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
              (t "pool.items.item.create.title")
              (t "pool.items.item.edit.title")))

         ($ :h3 {:className "text-sm mb-6 text-gray-500"}
            (if is-create
              (t "pool.items.item.create.description")
              (t "pool.items.item.edit.description")))

         ($ Card {:className "py-8 mb-12"}
            ($ CardContent
               ($ Scrollspy {:className "flex gap-4"}
                  ($ ScrollspyMenu)

                  ($ Form (merge form)
                     ($ :form {:id "item-form"
                               :className "space-y-12 w-full lg:w-3/5"
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
                               ($ form-fields/field {:key (:name block)
                                                     :control control
                                                     :form form
                                                     :block block}))))))

                  ($ ButtonGroup {:class-name "ml-auto sticky self-end bottom-[1.5rem]"}
                     ($ Button {:type "submit"
                                :form "item-form"}
                        (if is-create
                          (str (when batch?
                                 (str field-count " x "))
                               (t "pool.items.item.create.submit"))
                          (t "pool.items.item.edit.submit")))

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
                                      (t "pool.items.item.create.cancel")
                                      (t "pool.items.item.edit.cancel")))))))))))))))
