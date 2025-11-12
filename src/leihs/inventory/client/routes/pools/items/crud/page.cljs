(ns leihs.inventory.client.routes.pools.items.crud.page
  (:require
   ["@/components/react/scrollspy/scrollspy" :refer [Scrollspy ScrollspyItem
                                                     ScrollspyMenu]]
   ["@/routes/pools/items/crud/form" :refer [schema structure]]
   ["@@/alert-dialog" :refer [AlertDialog AlertDialogAction AlertDialogCancel
                              AlertDialogContent AlertDialogDescription
                              AlertDialogFooter AlertDialogHeader
                              AlertDialogTitle]]
   ["@@/button" :refer [Button]]
   ["@@/button-group" :refer [ButtonGroup ButtonGroupSeparator]]
   ["@@/card" :refer [Card CardContent]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuTrigger
                               DropdownMenuContent DropdownMenuItem
                               DropdownMenuSeparator DropdownMenuGroup]]
   ["@@/form" :refer [Form]]
   ["@@/spinner" :refer [Spinner]]
   ["@hookform/resolvers/zod" :refer [zodResolver]]
   ["lucide-react" :refer [Trash ChevronDownIcon]]
   ["react" :as react]
   ["react-hook-form" :refer [useForm useWatch]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [Link useLoaderData]]
   ["sonner" :refer [toast]]
   ["zod" :as z]
   [cljs.core.async :as async :refer [go <!]]
   [cljs.core.async.interop :refer-macros [<p!]]
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.fields-to-form :as fields-to-form]
   [leihs.inventory.client.lib.fields-to-zod :as fields-to-zod]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.pools.items.crud.components.fields :as form-fields]
   [leihs.inventory.client.routes.pools.items.crud.core :as core]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defn- on-invalid [data]
  (.. toast (error "Invalid Data"))
  (js/console.debug "is invalid: " data))

;; (def default-values (cj {:number-items 1
;;                          :inventory-code ""
;;                          :retired "yes"
;;                          :retire-reason ""
;;                          :working "ok"
;;                          :availability "ok"
;;                          :lendable "ok"
;;                          :models {:id "" :name ""}}))

(defui page []
  (let [[t] (useTranslation)
        location (router/useLocation)
        navigate (router/useNavigate)

        state (.. location -state)
        is-create (.. location -pathname (includes "create"))
        is-delete (.. location -pathname (includes "delete"))

        is-edit (not (or is-create is-delete))

        {:keys [data]} (jc (useLoaderData))

        ;; Transform fields data to form structure
        structure (fields-to-form/transform-fields-to-structure data)

        ;; Extract default values from fields
        defaults (when data
                   (cj (fields-to-form/extract-default-values data)))

        form (useForm #js {:resolver (zodResolver (fields-to-zod/fields-to-zod-schema data))
                           :defaultValues defaults})

        get-values (.. form -getValues)
        ;; reactive values
        is-retired (get-values "retired")
        ;; reactive values end

        is-loading (.. form -formState -isLoading)

        control (.. form -control)
        params (router/useParams)

        handle-submit (.. form -handleSubmit)
        handle-delete (fn [] (go))

        on-submit (fn [submit-data event]
                    (go
                      (let [attachments (if is-create
                                          (:attachments (jc submit-data))
                                          (filter (fn [el] (= (:id el) nil))
                                                  (:attachments (jc submit-data))))

                            attachments-to-delete (if is-edit
                                                    (->> (:attachments data)
                                                         (map :id)
                                                         (remove (set (map :id (:attachments (jc submit-data))))))
                                                    nil)

                            item-data (into {} (dissoc (jc submit-data) :attachments))

                            pool-id (aget params "pool-id")

                            item-res (if is-create
                                       (<p! (-> http-client
                                                (.post (str "/inventory/" pool-id "/items/")
                                                       (js/JSON.stringify (cj item-data))
                                                       (cj {:cache
                                                            {:update {:models "delete"
                                                                      :compatible-models "delete"
                                                                      :manufacturers "delete"}}}))

                                                (.then (fn [res]
                                                         {:status (.. res -status)
                                                          :statusText (.. res -statusText)
                                                          :id (.. res -data -id)}))
                                                (.catch (fn [err]
                                                          {:status (.. err -response -status)
                                                           :statusText (.. err -response -statusText)}))))

                                       (<p! (let [item-id (aget params "item-id")]
                                              (-> http-client
                                                  (.patch (str "/inventory/" pool-id "/items/" item-id)
                                                          (js/JSON.stringify (cj item-data))
                                                          (cj {:cache
                                                               {:update {:models "delete"
                                                                         (keyword item-id) "delete"}}}))
                                                  (.then (fn [res]
                                                           {:status (.. res -status)
                                                            :statusText (.. res -statusText)
                                                            :id (.. res -data -id)}))
                                                  (.catch (fn [err]
                                                            {:status (.. err -response -status)
                                                             :statusText (.. err -response -statusText)}))))))

                            item-id (when (not= (:status item-res) "200") (:id item-res))]

                        (.. event (preventDefault))

                        (when attachments-to-delete
                          (doseq [attachment-id attachments-to-delete]
                            ;; delete attachments that are not in the new model
                            (<p! (-> http-client
                                     (.delete (str "/inventory/" pool-id "/models/" item-id "/attachments/" attachment-id))
                                     (.then #(.-data %))))))

                        (if (not= (:status item-res) 200)
                          (.. toast (error (t (str "pool.model.create." (:status item-res)))))

                          (do
                            ;; upload attachments sequentially
                            (doseq [attachment attachments]
                              (let [file (:file attachment)
                                    binary-data (<p! (.. file (arrayBuffer)))
                                    type (.. file -type)
                                    name (.. file -name)]

                                (<p! (-> http-client
                                         (.post (str "/inventory/" pool-id "/models/" item-id "/attachments/")
                                                binary-data
                                                (cj {:headers {"Content-Type" type
                                                               "X-Filename" name}}))))))

                            (if is-create
                              (.. toast (success (t "pool.model.create.success")))
                              (.. toast (success (t "pool.model.edit.success"))))

                            ;; state needs to be forwarded for back navigation
                            (if is-create
                              (navigate (str "/inventory/" pool-id "/list"
                                             (some-> state .-searchParams))
                                        #js {:state state
                                             :viewTransition true})

                              (navigate (str "/inventory/" pool-id "/list"
                                             (some-> state .-searchParams))
                                        #js {:state state
                                             :viewTransition true})))))))]

    (uix/use-effect
     (fn []
       (when (and is-create (not is-loading))
         (let [owner (.. js/document (querySelector "[name='owner_id']"))]
           (set! (.. owner -disabled) true))))
     [is-create is-loading])

    (if is-loading
      ($ :div {:className "flex justify-center items-center h-screen"}
         ($ Spinner))

      ($ :article
         ($ :h1 {:className "text-2xl bold font-bold mt-12 mb-2"}
            (if is-create
              (t "pool.items.item.create.title")
              (t "pool.items.item.title")))

         ($ :h3 {:className "text-sm mb-6 text-gray-500"}
            (if is-create
              (t "pool.items.item.create.description")
              (t "pool.items.item.description")))

         ($ Card {:className "py-8 mb-12"}
            ($ CardContent
               ($ Scrollspy {:className "flex gap-4"}
                  ($ ScrollspyMenu)

                  ($ Form (merge form)
                     ($ :form {:id "create-model"
                               :no-validate true
                               :className "space-y-12 w-full lg:w-3/5"
                               :on-submit (handle-submit on-submit on-invalid)}

                        (for [section structure]
                          ($ ScrollspyItem {:className "scroll-mt-[10vh]"
                                            :key (:title section)
                                            :id (:title section)
                                            :name (:title section)}

                             ($ :h2 {:className "text-lg"} (:title section))
                             ($ :hr {:className "mb-4"})

                             (for [block (:blocks section)]
                               ($ form-fields/field {:key (:name block)
                                                     :control control
                                                     :form form
                                                     :block block}))))))

                  ($ ButtonGroup {:class-name "h-fit sticky top-[200px]"}
                     ($ Button {:type "submit"
                                :form "create-model"
                                :className "self-center"}
                        (if is-create
                          (t "pool.items.item.create.submit")
                          (t "pool.items.item.submit")))
                     ($ ButtonGroupSeparator)
                     ($ DropdownMenu
                        ($ DropdownMenuTrigger {:asChild true}
                           ($ Button {:className "self-center !px-2"}
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
                                      (t "pool.items.item.cancel")))))

                           ($ DropdownMenuSeparator)

                           ($ DropdownMenuGroup
                              (when (not is-create)
                                ($ DropdownMenuItem {:variant "destructive"
                                                     :asChild true}
                                   ($ Link {:to (router/generatePath "/inventory/:pool-id/items/:item-id/delete" params)
                                            :state state}
                                      "Delete")))))))

                  (when (not is-create)
                    ($ AlertDialog {:open is-delete}
                       ($ AlertDialogContent

                          ($ AlertDialogHeader
                             ($ AlertDialogTitle (t "pool.items.item.delete.title"))
                             ($ AlertDialogDescription (t "pool.items.item.delete.description")))

                          ($ AlertDialogFooter
                             ($ AlertDialogAction {:class-name "bg-destructive text-destructive-foreground 
                                                    hover:bg-destructive hover:text-destructive-foreground"
                                                   :onClick handle-delete}
                                (t "pool.items.item.delete.confirm"))
                             ($ AlertDialogCancel
                                ($ Link {:to (router/generatePath "/inventory/:pool-id/items/:item-id" params)
                                         :state state}

                                   (t "pool.items.item.delete.cancel")))))))
                  #_($ :div {:className "h-max flex space-x-6 sticky bottom-0 pt-12 lg:top-[43vh] ml-auto"}

                       ($ Link {:to (str (router/generatePath "/inventory/:pool-id/models" params)
                                         (some-> state .-searchParams))
                                :className "self-center hover:underline"
                                :viewTransition true}
                          (if is-create
                            (t "pool.model.create.cancel")
                            (t "pool.model.cancel")))

                       ($ Button {:type "submit"
                                  :form "create-model"
                                  :className "self-center"}
                          (if is-create
                            (t "pool.model.create.submit")
                            (t "pool.model.submit")))

                       (when (not is-create)
                         ($ Button {:asChild true
                                    :variant "destructive"
                                    :size "icon"
                                    :className "self-center"}
                            ($ Link {:to (router/generatePath "/inventory/:pool-id/items/:item-id/delete" params)
                                     :state state}
                               ($ Trash {:className "w-4 h-4"}))))

                   ;; Dialog when deleting a model
                       (when (not is-create)
                         ($ AlertDialog {:open is-delete}
                            ($ AlertDialogContent

                               ($ AlertDialogHeader
                                  ($ AlertDialogTitle (t "pool.model.delete.title"))
                                  ($ AlertDialogDescription (t "pool.model.delete.description")))

                               ($ AlertDialogFooter
                                  ($ AlertDialogAction {:class-name "bg-destructive text-destructive-foreground 
                                                    hover:bg-destructive hover:text-destructive-foreground"
                                                        :onClick handle-delete}
                                     (t "pool.model.delete.confirm"))
                                  ($ AlertDialogCancel
                                     ($ Link {:to (router/generatePath "/inventory/:pool-id/items/:item-id" params)
                                              :state state}

                                        (t "pool.model.delete.cancel")))))))))))))))

