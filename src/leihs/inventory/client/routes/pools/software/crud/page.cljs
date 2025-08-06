(ns leihs.inventory.client.routes.pools.software.crud.page
  (:require
   ["@/components/react/scrollspy/scrollspy" :refer [Scrollspy ScrollspyItem
                                                     ScrollspyMenu]]
   ["@/routes/pools/software/crud/form" :refer [schema structure]]
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
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.pools.software.crud.components.fields :as form-fields]
   [leihs.inventory.client.routes.pools.software.crud.core :as core]
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
                                            (fn [] (core/prepare-default-values data))
                                            (cj core/default-values))})

        is-loading (.. form -formState -isLoading)

        control (.. form -control)
        params (router/useParams)

        on-invalid (fn [data]
                     (let [invalid-filds-count (count (jc data))]
                       (if (= invalid-filds-count 0)
                         (.. toast (error (t "pool.software.create.invalid" #js {:count invalid-filds-count})))
                         (.. toast (error (t "pool.software.create.invalid" #js {:count invalid-filds-count}))))

                       (js/console.debug "is invalid: " data)))

        handle-submit (.. form -handleSubmit)
        handle-delete (fn []
                        (go
                          (let [pool-id (aget params "pool-id")
                                software-id (aget params "software-id")
                                res (<p! (-> http-client
                                             (.delete (str "/inventory/" pool-id "/software/" software-id))
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
                                (.. toast (success (t "pool.software.delete.success")))

                                ;; navigate to models list
                                (navigate (router/generatePath "/inventory/:pool-id/list" params)
                                          #js {:state state}))

                              ;; show error message
                              (.. toast (error (t "pool.software.delete.error")))))))

        on-submit (fn [submitted-data event]
                    (go
                      (let [attachments (if is-create
                                          (:attachments (jc submitted-data))
                                          (filter (fn [el] (= (:id el) nil))
                                                  (:attachments (jc submitted-data))))

                            attachments-to-delete (if is-edit
                                                    (->> (:attachments data)
                                                         (map :id)
                                                         (remove (set (map :id (:attachments (jc submitted-data))))))
                                                    nil)

                            ;; remove empty attachments from the data, 
                            ;; because they have their own endpoint
                            software-data (into {} (dissoc (jc submitted-data) :attachments))

                            pool-id (aget params "pool-id")

                            software-res (if is-create
                                           (<p! (-> http-client
                                                    (.post (str "/inventory/" pool-id "/software/")
                                                           (js/JSON.stringify (cj software-data))
                                                           (cj {:cache
                                                                {:update {:manufacturers "delete"}}}))

                                                    (.then (fn [res]
                                                             {:status (.. res -status)
                                                              :statusText (.. res -statusText)
                                                              :id (.. res -data -id)}))
                                                    (.catch (fn [err]
                                                              {:status (.. err -response -status)
                                                               :statusText (.. err -response -statusText)}))))

                                           (<p! (let [software-id (aget params "software-id")]
                                                  (-> http-client
                                                      (.put (str "/inventory/" pool-id "/software/" software-id)
                                                            (js/JSON.stringify (cj software-data))
                                                            (cj {:cache
                                                                 {:update {(keyword software-id) "delete"}}}))
                                                      (.then (fn [res]
                                                               {:status (.. res -status)
                                                                :statusText (.. res -statusText)
                                                                :id (.. res -data -id)}))
                                                      (.catch (fn [err]
                                                                {:status (.. err -response -status)
                                                                 :statusText (.. err -response -statusText)}))))))

                            software-id (when (not= (:status software-res) "200") (:id software-res))]

                        (.. event (preventDefault))

                        (when attachments-to-delete
                          (doseq [attachment-id attachments-to-delete]
                            ;; delete attachments that are not in the new model
                            (<p! (-> http-client
                                     (.delete (str "/inventory/" pool-id "/models/" software-id "/attachments/" attachment-id))
                                     (.then #(.-data %))))))

                        (if (not= (:status software-res) 200)
                          (.. toast (error (t (str "pool.software.create." (:status software-res)))))

                          (do
                            ;; upload attachments sequentially
                            (doseq [attachment attachments]
                              (let [file (:file attachment)
                                    binary-data (<p! (.. file (arrayBuffer)))
                                    type (.. file -type)
                                    name (.. file -name)]

                                (<p! (-> http-client
                                         (.post (str "/inventory/" pool-id "/models/" software-id "/attachments/")
                                                binary-data
                                                (cj {:headers {"Content-Type" type
                                                               "X-Filename" name}}))))))

                            (if is-create
                              (.. toast (success (t "pool.software.create.success")))
                              (.. toast (success (t "pool.software.edit.success"))))

                              ;; state needs to be forwarded for back navigation
                            (if is-create
                              (navigate (str (router/generatePath
                                              "/inventory/:pool-id/list"
                                              #js {:pool-id pool-id}) "?" (params-with-all-items))
                                        #js {:state state
                                             :viewTransition true})

                              (navigate (str (router/generatePath
                                              "/inventory/:pool-id/list"
                                              #js {:pool-id pool-id}) (some-> state .-searchParams))
                                        #js {:state state
                                             :viewTransition true})))))))]

    ;; (uix/use-effect
    ;;  (fn []
    ;;    (when (and is-edit (not is-loading))
    ;;      (let [package (.. js/document (querySelector "[data-id='is-package']"))]
    ;;        (set! (.. package -disabled) true))))
    ;;  [is-edit is-loading])

    (if is-loading
      ($ :div {:className "flex justify-center items-center h-screen"}
         ($ Spinner))

      ($ :article
         ($ :h1 {:className "text-2xl bold font-bold mt-12 mb-2"}
            (if is-create
              (t "pool.software.create.title")
              (t "pool.software.title")))

         ($ :h3 {:className "text-sm mb-6 text-gray-500"}
            (if is-create
              (t "pool.software.create.description")
              (t "pool.software.description")))

         ($ Card {:className "py-8 mb-12"}
            ($ CardContent
               ($ Scrollspy {:className "flex gap-4"}
                  ($ ScrollspyMenu)

                  ($ Form (merge form)
                     ($ :form {:id "create-software"
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
                                   :form "create-software"}
                           (if is-create
                             (t "pool.software.create.submit")
                             (t "pool.software.submit")))

                        ($ DropdownMenu
                           ($ DropdownMenuTrigger {:asChild true}
                              ($ Button {:data-test-id "submit-dropdown"
                                         :size "icon"}
                                 ($ ChevronDown {:className "w-4 h-4"})))
                           ($ DropdownMenuContent {:align "end"}
                              ($ DropdownMenuItem {:asChild true}
                                 ($ Link {:to (str (router/generatePath "/inventory/:pool-id/list" params)
                                                   (some-> state .-searchParams))
                                          :viewTransition true}
                                    (if is-create
                                      (t "pool.software.create.cancel")
                                      (t "pool.software.cancel"))))

                              (when (not is-create)
                                ($ DropdownMenuItem {:asChild true}
                                   ($ Link {:to (router/generatePath "/inventory/:pool-id/software/:software-id/delete" params)
                                            :state state}
                                      (t "pool.software.edit.delete")))))))

                      ;; Dialog when deleting a software
                     (when (not is-create)
                       ($ AlertDialog {:open is-delete}
                          ($ AlertDialogContent

                             ($ AlertDialogHeader
                                ($ AlertDialogTitle (t "pool.software.delete.title"))
                                ($ AlertDialogDescription (t "pool.software.delete.description")))

                             ($ AlertDialogFooter
                                ($ AlertDialogAction {:class-name "bg-destructive text-destructive-foreground 
                                                    hover:bg-destructive hover:text-destructive-foreground"
                                                      :onClick handle-delete}
                                   (t "pool.software.delete.confirm"))

                                ($ AlertDialogCancel
                                   ($ Link {:to (router/generatePath "/inventory/:pool-id/software/:software-id" params)
                                            :state state}

                                      (t "pool.software.delete.cancel")))))))))))))))


