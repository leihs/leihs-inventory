(ns leihs.inventory.client.routes.pools.packages.crud.page
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
   [leihs.inventory.client.routes.pools.packages.crud.components.fields :as form-fields]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui page []
  (let [[t] (useTranslation)
        location (router/useLocation)
        navigate (router/useNavigate)

        state (.. location -state)
        is-create (.. location -pathname (includes "create"))
        is-delete (.. location -pathname (includes "delete"))

        is-edit (not (or is-create is-delete))

        {:keys [data model]} (jc (useLoaderData))

        ;; Define custom fields for items selection
        custom-fields [{:id "item_ids"
                        :type "array"
                        :component "items"
                        :group "Content"
                        :group-after "Mandatory data"
                        :required false
                        :default []
                        :props {:text {:select "pool.packages.package.fields.items.select"
                                       :search "pool.packages.package.fields.items.search"
                                       :searching "pool.packages.package.fields.items.searching"
                                       :search_empty "pool.packages.package.fields.items.search_empty"
                                       :not_found "pool.packages.package.fields.items.not_found"}}
                        :validator (-> (z/array (z/object (cj {:id (z/guid)})))
                                       (.transform (fn [arr] (mapv (fn [item] (.-id item)) arr))))}]

        ;; Merge custom fields with API fields
        fields (concat (:fields data) custom-fields)
        ;; Transform fields data to form structure
        structure (dynamic-form/fields->structure fields)

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

        building (useWatch (cj {:control control
                                :name "building_id.value"}))

        building-is-dirty? (:isDirty (jc (get-field-state "building_id")))

        on-invalid (fn [data]
                     (let [invalid-fields-count (count (jc data))]
                       (.. toast (error (if is-create
                                          (t "pool.packages.package.create.invalid"
                                             #js {:count invalid-fields-count})
                                          (t "pool.packages.package.edit.invalid"
                                             #js {:count invalid-fields-count}))))

                       (js/console.debug "is invalid: " data)))

        handle-submit (.. form -handleSubmit)
        on-submit (fn [submit-data event]
                    (js/console.debug (jc submit-data))

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

                            item-data (jc submit-data)

                            pool-id (aget params "pool-id")
                            package-id (aget params "package-id")

                            item-res (if is-create
                                       (<p! (-> http-client
                                                (.post (str "/inventory/" pool-id "/items/")
                                                       (js/JSON.stringify (cj item-data)))

                                                (.then (fn [res]
                                                         {:status (.. res -status)
                                                          :statusText (.. res -statusText)
                                                          :id (.. res -data -id)}))
                                                (.catch (fn [err]
                                                          {:status (.. err -response -status)
                                                           :data (jc (.. err -response -data))
                                                           :statusText (.. err -response -statusText)}))))

                                       (<p! (-> http-client
                                                (.patch (str "/inventory/" pool-id "/items/" package-id)
                                                        (js/JSON.stringify (cj item-data))
                                                        (cj {:cache
                                                             {:update {(keyword package-id) "delete"}}}))

                                                (.then (fn [res]
                                                         {:status (.. res -status)
                                                          :statusText (.. res -statusText)
                                                          :id (.. res -data -id)}))
                                                (.catch (fn [err]
                                                          {:status (.. err -response -status)

                                                           :data (jc (.. err -response -data))
                                                           :statusText (.. err -response -statusText)})))))

                            item-id (if is-create
                                      (:id item-res)
                                      package-id)]

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
                                           (t "pool.packages.package.create.conflict")
                                           (t "pool.packages.package.edit.conflict"))
                                         (cj {:duration 20000
                                              :action
                                              {:label "Update"
                                               :onClick (fn []
                                                          (set-value "inventory_code" (:proposed_code (:data item-res))))}})))

                          500 (.. toast (error (if is-create
                                                 (t "pool.packages.package.create.error")
                                                 (t "pool.packages.package.edit.error"))))

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
                                                     (t "pool.packages.package.create.success")
                                                     (t "pool.packages.package.edit.success"))))

                                ;; state needs to be forwarded for back navigation
                                (if is-create
                                  (navigate (str "/inventory/" pool-id "/list"
                                                 (some-> state .-searchParams))
                                            #js {:state state
                                                 :viewTransition true})

                                  (navigate (str "/inventory/" pool-id "/list"
                                                 (some-> state .-searchParams))
                                            #js {:state state
                                                 :viewTransition true})))

                          ;; default
                          (.. toast (error :statusText item-res))))))]

;; Clear room_id when building changes
    (uix/use-effect
     (fn []
       (when (and building (not is-loading) building-is-dirty?)
         (set-value "room_id" nil)))
     [building is-loading set-value building-is-dirty?])

    (if is-loading
      ($ :div {:className "flex justify-center items-center h-screen"}
         ($ Spinner))

      ($ :article
         ($ :h1 {:className "text-2xl bold font-bold mt-12 mb-2"}
            (if is-create
              (t "pool.packages.package.create.title")
              (t "pool.packages.package.edit.title")))

         ($ :h3 {:className "text-sm mb-6 text-gray-500"}
            (if is-create
              (t "pool.packages.package.create.description")
              (t "pool.packages.package.edit.description")))

         ($ Card {:className "py-8 mb-12"}
            ($ CardContent
               ($ Scrollspy {:className "flex gap-4"}
                  ($ ScrollspyMenu)

                  ($ Form (merge form)
                     ($ :form {:id "package-form"
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
                                :form "package-form"}
                        (if is-create
                          (t "pool.packages.package.create.submit")
                          (t "pool.packages.package.edit.submit")))

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
                                      (t "pool.packages.package.create.cancel")
                                      (t "pool.packages.package.edit.cancel")))))))))))))))
