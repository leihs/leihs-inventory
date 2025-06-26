(ns leihs.inventory.client.routes.models.options.crud.page
  (:require
   ["@/components/react/scrollspy/scrollspy" :refer [Scrollspy ScrollspyItem
                                                     ScrollspyMenu]]
   ["@/routes/models/options/crud/form" :refer [schema structure]]
   ["@@/alert-dialog" :refer [AlertDialog AlertDialogAction AlertDialogCancel
                              AlertDialogContent AlertDialogDescription
                              AlertDialogFooter AlertDialogHeader
                              AlertDialogTitle]]
   ["@@/button" :refer [Button]]
   ["@@/card" :refer [Card CardContent]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent
                               DropdownMenuItem DropdownMenuSeparator
                               DropdownMenuTrigger]]
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
   [leihs.inventory.client.routes.models.options.crud.components.fields :as form-fields]
   [leihs.inventory.client.routes.models.options.crud.core :as core]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

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
                                                 (merge core/default-values (jc data))))
                                            (cj core/default-values))})

        is-loading (.. form -formState -isLoading)

        control (.. form -control)
        params (router/useParams)

        on-invalid (fn [data]
                     (let [invalid-filds-count (count (jc data))]
                       (if (= invalid-filds-count 0)
                         (.. toast (error (t "pool.option.create.invalid" #js {:count invalid-filds-count})))
                         (.. toast (error (t "pool.option.create.invalid" #js {:count invalid-filds-count}))))

                       (js/console.debug "is invalid: " data)))

        handle-submit (.. form -handleSubmit)
        handle-delete (fn []
                        (go
                          (let [pool-id (aget params "pool-id")
                                option-id (aget params "option-id")
                                res (<p! (-> http-client
                                             (.delete (str "/inventory/" pool-id "/options/" option-id))
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
                                (.. toast (success (t "pool.option.delete.success")))

                                ;; navigate to models list
                                (navigate (router/generatePath "/inventory/:pool-id/models" params)
                                          #js {:state state}))

                              ;; show error message
                              (.. toast (error (t "pool.option.delete.error")))))))

        on-submit (fn [submit-data event]
                    (go
                      (let [pool-id (aget params "pool-id")

                            option-res (if is-create
                                         (<p! (-> http-client
                                                  (.post (str "/inventory/" pool-id "/options/")
                                                         (js/JSON.stringify (cj submit-data)))

                                                  (.then (fn [res]
                                                           {:status (.. res -status)
                                                            :statusText (.. res -statusText)
                                                            :id (.. res -data -id)}))
                                                  (.catch (fn [err]
                                                            {:status (.. err -response -status)
                                                             :statusText (.. err -response -statusText)}))))

                                         (<p! (let [option-id (aget params "option-id")]
                                                (-> http-client
                                                    (.put (str "/inventory/" pool-id "/options/" option-id)
                                                          (js/JSON.stringify (cj submit-data))
                                                          (cj {:cache
                                                               {:update {(keyword option-id) "delete"}}}))
                                                    (.then (fn [res]
                                                             {:status (.. res -status)
                                                              :statusText (.. res -statusText)
                                                              :id (.. res -data -id)}))
                                                    (.catch (fn [err]
                                                              {:status (.. err -response -status)
                                                               :statusText (.. err -response -statusText)}))))))]
                        (.. event (preventDefault))

                        (if (not= (:status option-res) 200)
                          (if is-create
                            (.. toast (error (t (str "pool.option.create." (:status option-res)))))
                            (.. toast (error (t (str "pool.option.edit." (:status option-res))))))

                          (do
                            ;; patch cover-image when needed
                            (if is-create
                              (.. toast (success (t "pool.option.create.success")))
                              (.. toast (success (t "pool.option.edit.success"))))

                              ;; state needs to be forwarded for back navigation
                            (if is-create
                              (navigate (str (router/generatePath
                                              "/inventory/:pool-id/models"
                                              #js {:pool-id pool-id}) "?" (params-with-all-items))
                                        #js {:state state
                                             :viewTransition true})

                              (navigate (str (router/generatePath
                                              "/inventory/:pool-id/models"
                                              #js {:pool-id pool-id}) (some-> state .-searchParams))
                                        #js {:state state
                                             :viewTransition true})))))))]

    (if is-loading
      ($ :div {:className "flex justify-center items-center h-screen"}
         ($ Spinner))

      ($ :article
         ($ :h1 {:className "text-2xl bold font-bold mt-12 mb-2"}
            (if is-create
              (t "pool.option.create.title")
              (t "pool.option.edit.title")))

         ($ :h3 {:className "text-sm mb-6 text-gray-500"}
            (if is-create
              (t "pool.option.create.description")
              (t "pool.option.edit.description")))

         ($ Card {:className "py-8 mb-12"}
            ($ CardContent
               ($ Scrollspy {:className "flex gap-4"}
                  ($ ScrollspyMenu)

                  ($ Form (merge form)
                     ($ :form {:id "create-option"
                               :className "space-y-12 w-full lg:w-3/5"
                               :on-submit (handle-submit on-submit on-invalid)}

                        (for [section (jc structure)]
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

                  ($ :div {:className "h-max flex space-x-6 sticky bottom-0 pt-12 lg:top-[43vh] ml-auto"}

                     ($ :div {:class-name "flex [&>*]:rounded-none [&>button:first-child]:rounded-l-md [&>button:last-child]:rounded-r-md divide-x divide-border/40"}
                        ($ Button {:type "submit"
                                   :form "create-option"}
                           (if is-create
                             (t "pool.option.create.submit")
                             (t "pool.option.edit.submit")))

                        ($ DropdownMenu
                           ($ DropdownMenuTrigger {:asChild true}
                              ($ Button {:data-test-id "submit-dropdown"
                                         :size "icon"}
                                 ($ ChevronDown {:className "w-4 h-4"})))
                           ($ DropdownMenuContent {:align "end"}
                              ($ DropdownMenuItem {:asChild true}
                                 ($ Link {:to (str (router/generatePath "/inventory/:pool-id/models" params)
                                                   (some-> state .-searchParams))
                                          :viewTransition true}
                                    (if is-create
                                      (t "pool.option.create.cancel")
                                      (t "pool.option.edit.cancel"))))

                              ;; currently disabled until decided if we want to allow deleting options
                              (when (not is-create)
                                ($ DropdownMenuItem {:asChild true}
                                   ($ Link {:to (router/generatePath "/inventory/:pool-id/options/:option-id/delete" params)
                                            :state state}
                                      "Delete"))))))

                     ;; Dialog when deleting a model
                     (when (not is-create)
                       ($ AlertDialog {:open is-delete}
                          ($ AlertDialogContent

                             ($ AlertDialogHeader
                                ($ AlertDialogTitle (t "pool.option.delete.title"))
                                ($ AlertDialogDescription (t "pool.option.delete.description")))

                             ($ AlertDialogFooter
                                ($ AlertDialogAction {:class-name "bg-destructive text-destructive-foreground 
                                                    hover:bg-destructive hover:text-destructive-foreground"
                                                      :onClick handle-delete}
                                   (t "pool.option.delete.confirm"))
                                ($ AlertDialogCancel
                                   ($ Link {:to (router/generatePath "/inventory/:pool-id/models/:option-id" params)
                                            :state state}

                                      (t "pool.option.delete.cancel")))))))))))))))


