(ns leihs.inventory.client.routes.pools.inventory.entitlement-groups.crud.page
  (:require
   ["@/routes/pools/inventory/entitlement_groups/crud/form" :refer [schema structure]]
   ["@@/alert-dialog" :refer [AlertDialog AlertDialogAction AlertDialogCancel
                              AlertDialogContent AlertDialogDescription
                              AlertDialogFooter AlertDialogHeader
                              AlertDialogTitle]]
   ["@@/button" :refer [Button]]
   ["@@/button-group" :refer [ButtonGroup ButtonGroupSeparator]]
   ["@@/card" :refer [Card CardContent]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent
                               DropdownMenuItem DropdownMenuTrigger]]
   ["@@/form" :refer [Form]]
   ["@@/spinner" :refer [Spinner]]
   ["@hookform/resolvers/zod" :refer [zodResolver]]
   ["lucide-react" :refer [ChevronDown]]
   ["react-hook-form" :refer [useWatch useForm]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [Link useLoaderData]]
   ["sonner" :refer [toast]]
   [cljs.core.async :as async :refer [go]]
   [cljs.core.async.interop :refer-macros [<p!]]
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.form-helper :as form-helper]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.pools.inventory.entitlement-groups.crud.components.fields :as form-fields]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(def default-values {:name ""
                     :models []})

(defui title [{:keys [control]}]
  (let [name (useWatch (clj->js {:control control
                                 :name "name"}))]
    ($ :<> (when (> (count name) 0) (str " – " name)))))

(defui page []
  (let [[t] (useTranslation)
        location (router/useLocation)
        params (router/useParams)
        navigate (router/useNavigate)

        state (.. location -state) ;; to get search-params from pevious route 

        is-create (.. location -pathname (includes "create"))
        is-delete (.. location -pathname (includes "delete"))
        is-edit (not (or is-create is-delete))

        data (-> (useLoaderData)
                 :data
                 (#(-> % (assoc :users (:direct_users %)) (dissoc :direct_users))))

        form (useForm #js {:resolver (zodResolver schema)
                           :defaultValues (if is-edit
                                            (cj (form-helper/replace-nil-values
                                                 (merge default-values (jc data))))
                                            (cj default-values))})

        is-loading (.. form -formState -isLoading)
        form-control (.. form -control)

        handle-submit (.. form -handleSubmit)
        on-invalid (fn [data]
                     (let [invalid-fields-count (count (jc data))]
                       (if (= invalid-fields-count 0)
                         (.. toast (error (t "pool.entitlement_groups.entitlement_group.create.invalid" #js {:count invalid-fields-count})))
                         (.. toast (error (t "pool.entitlement_groups.entitlement_group.create.invalid" #js {:count invalid-fields-count}))))

                       (js/console.debug "is invalid: " data)))
        on-submit (fn [submitted-data event]
                    (go
                      (let [entitlement-group-data (jc submitted-data)

                            pool-id (aget params "pool-id")

                            entitlement-group-res
                            (if is-create
                              (<p! (-> http-client
                                       (.post (str "/inventory/" pool-id "/entitlement-groups/")
                                              (js/JSON.stringify (cj entitlement-group-data)))
                                       (.then (fn [res]
                                                {:status (.. res -status)
                                                 :statusText (.. res -statusText)
                                                 :id (.. res -data -id)}))
                                       (.catch (fn [err]
                                                 {:status (.. err -response -status)
                                                  :statusText (.. err -response -statusText)}))))

                              (<p! (let [entitlement-group-id (aget params "entitlement-group-id")]
                                     (-> http-client
                                         (.put (str "/inventory/" pool-id "/entitlement-groups/" entitlement-group-id)
                                               (js/JSON.stringify (cj entitlement-group-data))
                                               (cj {:cache
                                                    {:update {(keyword entitlement-group-id) "delete"}}}))
                                         (.then (fn [res]
                                                  {:status (.. res -status)
                                                   :statusText (.. res -statusText)
                                                   :id (.. res -data -id)}))
                                         (.catch (fn [err]
                                                   {:status (.. err -response -status)
                                                    :statusText (.. err -response -statusText)}))))))]

                        (.. event (preventDefault))

                        (case (:status entitlement-group-res)
                          500 (.. toast (error (t "pool.entitlement_groups.entitlement_group.create.error")))

                          200 (do
                                (if is-create
                                  (.. toast (success (t "pool.entitlement_groups.entitlement_group.create.success")))
                                  (.. toast (success (t "pool.entitlement_groups.entitlement_group.edit.success"))))

                                ;; state needs to be forwarded for back navigation
                                (if is-create
                                  (navigate (str (router/generatePath
                                                  "/inventory/:pool-id/entitlement-groups"
                                                  #js {:pool-id pool-id}) "?" (.. (js/URLSearchParams. (.-searchParams state)) (toString)))
                                            #js {:state state
                                                 :viewTransition true})

                                  (navigate (str (router/generatePath
                                                  "/inventory/:pool-id/entitlement-groups"
                                                  #js {:pool-id pool-id}) (some-> state .-searchParams))
                                            #js {:state state
                                                 :viewTransition true})))

                          ;; default
                          (.. toast (error (str "Status " (:status entitlement-group-res) " " (:statusText entitlement-group-res))))))))

        handle-delete (fn []
                        (go
                          (let [pool-id (aget params "pool-id")
                                entitlement-group-id (aget params "entitlement-group-id")
                                res (<p! (-> http-client
                                             (.delete (str "/inventory/" pool-id "/entitlement-groups/" entitlement-group-id))
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
                                (.. toast (success (t "pool.entitlement_groups.entitlement_group.delete.success")))

                                ;; navigate to models list
                                (navigate (router/generatePath "/inventory/:pool-id/entitlement-groups" params)
                                          #js {:state state}))

                              ;; show error message
                              (.. toast (error (t "pool.entitlement_groups.entitlement_group.delete.error")))))))]
    (if is-loading
      ($ :div {:className "flex justify-center items-center h-screen"}
         ($ Spinner))

      ($ :article
         ($ :h1 {:className "text-2xl bold font-bold mt-12 mb-2"}
            (t "pool.entitlement_groups.entitlement_group.title")

            ($ title {:control form-control}))

         ($ Card {:className "py-8 mb-12"}
            ($ CardContent

               ($ :div {:className "flex flex-col xl:flex-row gap-4"}

                  ($ Form (merge form)
                     ($ :form {:id "create-entitlement-group"
                               :className "space-y-12"
                               :on-submit (handle-submit on-submit on-invalid)}

                        ($ :div {:className "flex flex-col lg:flex-row gap-12"}
                           (for [[col-index col] (map-indexed vector (jc structure))]
                             ($ :div {:className (if (= 0 col-index) "lg:basis-[55%] grow" "lg:basis-[45%] grow") :key col-index}
                                (for [section col]
                                  ($ :div {:className (str "scroll-mt-[10vh] break-inside-avoid " (if (:title section) "pt-12" "pt-5"))
                                           :key (:title section)
                                           :id (:title section)
                                           :name (t (:title section))}
                                     ($ :h2 {:className "text-lg"} (t (:title section)))
                                     (when (:title section)
                                       ($ :hr {:className "mb-4"}))

                                     (for [block (:blocks section)]
                                       ($ form-fields/field {:key (:name block)
                                                             :control form-control
                                                             :form form
                                                             :block block})))))))))

                  ($ ButtonGroup {:class-name "ml-auto sticky self-end bottom-[1.5rem]"}
                     ($ Button {:type "submit"
                                :form "create-entitlement-group"}
                        (if is-create
                          (t "pool.entitlement_groups.entitlement_group.create.submit")
                          (t "pool.entitlement_groups.entitlement_group.edit.submit")))

                     ($ ButtonGroupSeparator)

                     ($ DropdownMenu
                        ($ DropdownMenuTrigger {:asChild true}
                           ($ Button {:data-test-id "submit-dropdown"
                                      :size "icon"}
                              ($ ChevronDown {:className "w-4 h-4"})))
                        ($ DropdownMenuContent {:align "end"}
                           ($ DropdownMenuItem {:asChild true}
                              ($ Link {:to (str (router/generatePath "/inventory/:pool-id/entitlement-groups" params)
                                                (some-> state .-searchParams))
                                       :viewTransition true}
                                 (if is-create
                                   (t "pool.entitlement_groups.entitlement_group.create.cancel")
                                   (t "pool.entitlement_groups.entitlement_group.edit.cancel"))))

                           (when (not is-create)
                             ($ DropdownMenuItem {:asChild true}
                                ($ Link {:to (router/generatePath "/inventory/:pool-id/entitlement-groups/:entitlement-group-id/delete" params)
                                         :state state}
                                   (t "pool.entitlement_groups.entitlement_group.edit.delete")))))))

                  ; Dialog when deleting a entitlement group
                  (when (not is-create)
                    ($ AlertDialog {:open is-delete}
                       ($ AlertDialogContent

                          ($ AlertDialogHeader
                             ($ AlertDialogTitle (t "pool.entitlement_groups.entitlement_group.delete.title"))
                             ($ AlertDialogDescription (t "pool.entitlement_groups.entitlement_group.delete.description")))

                          ($ AlertDialogFooter
                             ($ AlertDialogAction {:class-name "bg-destructive text-destructive-foreground 
                                                                      hover:bg-destructive hover:text-destructive-foreground"
                                                   :onClick handle-delete}
                                (t "pool.entitlement_groups.entitlement_group.delete.confirm"))

                             ($ AlertDialogCancel
                                ($ Link {:to (router/generatePath "/inventory/:pool-id/entitlement-groups/:entitlement-group-id" params)
                                         :state state}

                                   (t "pool.entitlement_groups.entitlement_group.delete.cancel"))))))))))))))

