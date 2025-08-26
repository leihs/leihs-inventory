(ns leihs.inventory.client.routes.pools.inventory.templates.crud.page
  (:require
   ["@/routes/pools/inventory/templates/crud/form" :refer [schema structure]]
   ["@@/alert-dialog" :refer [AlertDialog AlertDialogAction AlertDialogCancel
                              AlertDialogContent AlertDialogDescription
                              AlertDialogFooter AlertDialogHeader
                              AlertDialogTitle]]
   ["@@/button" :refer [Button]]
   ["@@/card" :refer [Card CardContent]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent
                               DropdownMenuItem DropdownMenuTrigger]]
   ["@@/form" :refer [Form]]
   ["@@/spinner" :refer [Spinner]]
   ["@hookform/resolvers/zod" :refer [zodResolver]]
   ["lucide-react" :refer [ChevronDown]]
   ["react-hook-form" :refer [useForm]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [Link useLoaderData]]
   ["sonner" :refer [toast]]
   [cljs.core.async :as async :refer [go]]
   [cljs.core.async.interop :refer-macros [<p!]]
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.form-helper :as form-helper]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.pools.inventory.templates.crud.components.fields :as form-fields]
   [leihs.inventory.client.routes.pools.inventory.templates.crud.components.title :as title]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(def default-values {:name ""
                     :models []})

(defui page []
  (let [[t] (useTranslation)
        location (router/useLocation)
        navigate (router/useNavigate)

        ;; state to get search-params from pevious route
        state (.. location -state)
        ;; remove "with_items" from the URL params
        params-with-all-items (fn []
                                (let [params (js/URLSearchParams. (.-searchParams state))]
                                  (.delete params "with_items")
                                  (.. params (toString))))

        is-create (.. location -pathname (includes "create"))
        is-delete (.. location -pathname (includes "delete"))

        is-edit (not (or is-create is-delete))

        {:keys [data]} (useLoaderData)
        form (useForm #js {:resolver (zodResolver schema)
                           :defaultValues (if is-edit
                                            (cj (form-helper/replace-nil-values
                                                 (merge default-values (jc data))))
                                            (cj default-values))})

        get-values (aget form "getValues")

        is-loading (.. form -formState -isLoading)

        control (.. form -control)
        params (router/useParams)

        on-invalid (fn [data]
                     (let [invalid-filds-count (count (jc data))]
                       (if (= invalid-filds-count 0)
                         (.. toast (error (t "pool.templates.template.create.invalid" #js {:count invalid-filds-count})))
                         (.. toast (error (t "pool.templates.template.create.invalid" #js {:count invalid-filds-count}))))

                       (js/console.debug "is invalid: " data)))

        handle-submit (.. form -handleSubmit)
        handle-delete (fn []
                        (go
                          (let [pool-id (aget params "pool-id")
                                template-id (aget params "template-id")
                                res (<p! (-> http-client
                                             (.delete (str "/inventory/" pool-id "/templates/" template-id))
                                             (.then (fn [data]
                                                      {:status (.. data -status)
                                                       :statusText (.. data -statusText)
                                                       :data (.. data -data)}))
                                             (.catch (fn [err]
                                                       {:status (.. err -response -status)
                                                        :statusText (.. err -response -statusText)}))))
                                status (:status res)]

                            (if (= status 200)
                              (do
                                (.. toast (success (t "pool.templates.template.delete.success")))

                                ;; navigate to models list
                                (navigate (router/generatePath "/inventory/:pool-id/templates" params)
                                          #js {:state state}))

                              ;; show error message
                              (.. toast (error (t "pool.templates.template.delete.error")))))))

        on-submit (fn [submitted-data event]
                    (go
                      (let [;; remove empty attachments from the data, 
                            ;; because they have their own endpoint
                            template-data (into {} (jc submitted-data))

                            pool-id (aget params "pool-id")

                            template-res (if is-create
                                           (<p! (-> http-client
                                                    (.post (str "/inventory/" pool-id "/templates/")
                                                           (js/JSON.stringify (cj template-data)))
                                                    (.then (fn [res]
                                                             {:status (.. res -status)
                                                              :statusText (.. res -statusText)
                                                              :id (.. res -data -id)}))
                                                    (.catch (fn [err]
                                                              {:status (.. err -response -status)
                                                               :statusText (.. err -response -statusText)}))))

                                           (<p! (let [template-id (aget params "template-id")]
                                                  (-> http-client
                                                      (.put (str "/inventory/" pool-id "/templates/" template-id)
                                                            (js/JSON.stringify (cj template-data))
                                                            (cj {:cache
                                                                 {:update {(keyword template-id) "delete"}}}))
                                                      (.then (fn [res]
                                                               {:status (.. res -status)
                                                                :statusText (.. res -statusText)
                                                                :id (.. res -data -id)}))
                                                      (.catch (fn [err]
                                                                {:status (.. err -response -status)
                                                                 :statusText (.. err -response -statusText)}))))))]

                        (.. event (preventDefault))

                        (case (:status template-res)
                          409 (.. toast (error (t "pool.templates.template.create.conflict")))
                          500 (.. toast (error (t "pool.templates.template.create.error")))

                          200 (do
                                (if is-create
                                  (.. toast (success (t "pool.templates.template.create.success")))
                                  (.. toast (success (t "pool.templates.template.edit.success"))))

                                ;; state needs to be forwarded for back navigation
                                (if is-create
                                  (navigate (str (router/generatePath
                                                  "/inventory/:pool-id/templates"
                                                  #js {:pool-id pool-id}) "?" (params-with-all-items))
                                            #js {:state state
                                                 :viewTransition true})

                                  (navigate (str (router/generatePath
                                                  "/inventory/:pool-id/templates"
                                                  #js {:pool-id pool-id}) (some-> state .-searchParams))
                                            #js {:state state
                                                 :viewTransition true})))

                          :default (.. toast (error :statusText template-res))))))]

    (if is-loading
      ($ :div {:className "flex justify-center items-center h-screen"}
         ($ Spinner))

      ($ :article {:className "mt-6"}
         ($ :h1 {:className "text-2xl bold font-bold mt-12 mb-2"}
            (t "pool.templates.template.title")

            ($ title/main {:control control}))

         ($ Card {:className "my-4"}
            ($ CardContent
               ($ :div {:className "flex gap-4"}
                  ($ Form (merge form)
                     ($ :form {:id "create-template"
                               :className "space-y-12 w-full xl:w-3/5"
                               :on-submit (handle-submit on-submit on-invalid)}

                        (for [section (jc structure)]
                          ($ :div {:className "scroll-mt-[10vh]"
                                   :key (:title section)
                                   :id (:title section)
                                   :name (t (:title section))}

                             ($ :h2 {:className "text-lg"} (t (:title section)))
                             (when (:title section)
                               ($ :hr {:className "mb-4"}))

                             (for [block (:blocks section)]
                               ($ form-fields/field {:key (:name block)
                                                     :control control
                                                     :form form
                                                     :block block}))))))

                  ($ :div {:className "flex items-end flex-1"}
                     ($ :div {:class-name "flex [&>*]:rounded-none ml-auto sticky bottom-[1.5rem] [&>button:first-child]:rounded-l-md [&>button:last-child]:rounded-r-md divide-x divide-border/40"}
                        ($ Button {:type "submit"
                                   :form "create-template"}
                           (if is-create
                             (t "pool.templates.template.create.submit")
                             (t "pool.templates.template.edit.submit")))

                        ($ DropdownMenu
                           ($ DropdownMenuTrigger {:asChild true}
                              ($ Button {:data-test-id "submit-dropdown"
                                         :size "icon"}
                                 ($ ChevronDown {:className "w-4 h-4"})))
                           ($ DropdownMenuContent {:align "end"}
                              ($ DropdownMenuItem {:asChild true}
                                 ($ Link {:to (str (router/generatePath "/inventory/:pool-id/templates" params)
                                                   (some-> state .-searchParams))
                                          :viewTransition true}
                                    (if is-create
                                      (t "pool.templates.template.create.cancel")
                                      (t "pool.templates.template.edit.cancel"))))

                              (when (not is-create)
                                ($ DropdownMenuItem {:asChild true}
                                   ($ Link {:to (router/generatePath "/inventory/:pool-id/templates/:template-id/delete" params)
                                            :state state}
                                      (t "pool.templates.template.edit.delete")))))))

                      ;; Dialog when deleting a template
                     (when (not is-create)
                       ($ AlertDialog {:open is-delete}
                          ($ AlertDialogContent

                             ($ AlertDialogHeader
                                ($ AlertDialogTitle (t "pool.templates.template.delete.title"))
                                ($ AlertDialogDescription (t "pool.templates.template.delete.description")))

                             ($ AlertDialogFooter
                                ($ AlertDialogAction {:class-name "bg-destructive text-destructive-foreground 
                                                    hover:bg-destructive hover:text-destructive-foreground"
                                                      :onClick handle-delete}
                                   (t "pool.templates.template.delete.confirm"))

                                ($ AlertDialogCancel
                                   ($ Link {:to (router/generatePath "/inventory/:pool-id/templates/:template-id" params)
                                            :state state}

                                      (t "pool.templates.template.delete.cancel")))))))))))))))


