(ns leihs.inventory.client.routes.pools.models.crud.page
  (:require
   ["@/components/react/scrollspy/scrollspy" :refer [Scrollspy ScrollspyItem
                                                     ScrollspyMenu]]
   ["@/routes/pools/models/crud/form" :refer [schema structure]]
   ["@@/alert-dialog" :refer [AlertDialog AlertDialogAction AlertDialogCancel
                              AlertDialogContent AlertDialogDescription
                              AlertDialogFooter AlertDialogHeader
                              AlertDialogTitle]]
   ["@@/button" :refer [Button]]
   ["@@/button-group" :refer [ButtonGroup ButtonGroupSeparator]]
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
   [leihs.inventory.client.components.typo :refer [Typo]]
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.form-helper :as form-helper]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.pools.models.crud.components.fields :as form-fields]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(def default-values {:product ""
                     :is_package false
                     :manufacturer ""
                     :description ""
                     :internal_description ""
                     :technical_detail ""
                     :hand_over_note ""
                     :version ""
                     :categories []
                     :entitlements []
                     :properties []
                     :accessories []})

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

        {:keys [data]} (jc (useLoaderData))
        form (useForm #js {:resolver (zodResolver schema)
                           :defaultValues (if is-edit
                                            (fn [] (form-helper/process-files data :attachments :images))
                                            (cj default-values))})

        is-loading (.. form -formState -isLoading)

        control (.. form -control)
        params (router/useParams)
        on-invalid (fn [data]
                     (let [invalid-filds-count (count (jc data))]
                       (if (= invalid-filds-count 0)
                         (.. toast (error (t "pool.model.create.invalid" #js {:count invalid-filds-count})))
                         (.. toast (error (t "pool.model.create.invalid" #js {:count invalid-filds-count}))))

                       (js/console.debug "is invalid: " data)))

        handle-submit (.. form -handleSubmit)
        handle-delete (fn []
                        (go
                          (let [pool-id (aget params "pool-id")
                                model-id (aget params "model-id")
                                res (<p! (-> http-client
                                             (.delete (str "/inventory/" pool-id "/models/" model-id))
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
                                (.. toast (success (t "pool.model.delete.success")))

                                ;; navigate to models list
                                (navigate (router/generatePath "/inventory/:pool-id/list" params)
                                          #js {:state state}))

                              ;; show error message
                              (.. toast (error (t (str "pool.model.delete." (:status res)))))))))

        on-submit (fn [submit-data event]
                    (go
                      (let [images (if is-create
                                     (:images (jc submit-data))
                                     (filter (fn [el] (= (:id el) nil))
                                             (:images (jc submit-data))))

                            image-id (atom nil)

                            images-to-delete (if is-edit
                                               (->> (:images data)
                                                    (map :id)
                                                    (remove (set (map :id (:images (jc submit-data))))))
                                               nil)

                            attachments (if is-create
                                          (:attachments (jc submit-data))
                                          (filter (fn [el] (= (:id el) nil))
                                                  (:attachments (jc submit-data))))

                            attachments-to-delete (if is-edit
                                                    (->> (:attachments data)
                                                         (map :id)
                                                         (remove (set (map :id (:attachments (jc submit-data))))))
                                                    nil)

                            model-data (into {} (dissoc (jc submit-data) :images :attachments))

                            pool-id (aget params "pool-id")

                            model-res (if is-create
                                        (<p! (-> http-client
                                                 (.post (str "/inventory/" pool-id "/models/")
                                                        (js/JSON.stringify (cj model-data))
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

                                        (<p! (let [model-id (aget params "model-id")]
                                               (-> http-client
                                                   (.put (str "/inventory/" pool-id "/models/" model-id)
                                                         (js/JSON.stringify (cj model-data))
                                                         (cj {:cache
                                                              {:update {:models "delete"
                                                                        (keyword model-id) "delete"
                                                                        :compatible-models "delete"
                                                                        :manufacturers "delete"}}}))
                                                   (.then (fn [res]
                                                            {:status (.. res -status)
                                                             :statusText (.. res -statusText)
                                                             :id (.. res -data -id)}))
                                                   (.catch (fn [err]
                                                             {:status (.. err -response -status)
                                                              :statusText (.. err -response -statusText)}))))))

                            model-id (when (not= (:status model-res) "200") (:id model-res))]

                        (.. event (preventDefault))

                        (when images-to-delete
                          (doseq [image-id images-to-delete]
                            ;; delete images that are not in the new model
                            (<p! (-> http-client
                                     (.delete (str "/inventory/" pool-id "/models/" model-id "/images/" image-id))
                                     (.then #(.-data %))))))

                        (when attachments-to-delete
                          (doseq [attachment-id attachments-to-delete]
                            ;; delete attachments that are not in the new model
                            (<p! (-> http-client
                                     (.delete (str "/inventory/" pool-id "/models/" model-id "/attachments/" attachment-id))
                                     (.then #(.-data %))))))

                        (if (not= (:status model-res) 200)
                          (.. toast (error (t (str "pool.model.create." (:status model-res)))))

                          (do
                            ;; upload images sequentially and path model with is_cover when is needed + images with target_type
                            (doseq [image images]
                              (let [file (:file image)
                                    is-cover (:is_cover image)
                                    type (.. file -type)
                                    name (.. file -name)
                                    binary-data (<p! (.. file (arrayBuffer)))
                                    image-res (<p! (-> http-client
                                                       (.post (str "/inventory/" pool-id "/models/" model-id "/images/")
                                                              binary-data
                                                              (cj {:headers {"Content-Type" type
                                                                             "X-Filename" name}}))
                                                       (.then #(.-data %))))]

                                (when is-cover
                                  (reset! image-id (.. image-res -image -id)))

                                ;; patch image with target_type "Model"
                                #_(<p! (js/fetch (str "/inventory/" model-id "/images/" image-id)
                                                 (cj {:method "PATCH"
                                                      :headers {"Accept" "application/json"}
                                                      :body (js/JSON.stringify (cj {:target_type "Model"}))})))))

                            ;; upload attachments sequentially
                            (doseq [attachment attachments]
                              (let [file (:file attachment)
                                    binary-data (<p! (.. file (arrayBuffer)))
                                    type (.. file -type)
                                    name (.. file -name)]

                                (<p! (-> http-client
                                         (.post (str "/inventory/" pool-id "/models/" model-id "/attachments/")
                                                binary-data
                                                (cj {:headers {"Content-Type" type
                                                               "X-Filename" name}}))))))

                            ;; patch cover-image when needed
                            (let [cover-image (filter #(= (:is_cover %) true) (:images (jc submit-data)))
                                  cover-image-id (or (:id (first cover-image)) @image-id)]

                              (when cover-image-id
                                (<p! (-> http-client
                                         (.patch (str "/inventory/" pool-id "/models/" model-id)
                                                 (js/JSON.stringify (cj {:is_cover cover-image-id})))))))

                            (if is-create
                              (.. toast (success (t "pool.model.create.success")))
                              (.. toast (success (t "pool.model.edit.success"))))

                              ;; state needs to be forwarded for back navigation
                            (let [submit-type (aget event "nativeEvent" "submitter" "value")]
                              (if (= submit-type "save-add-item")
                                (navigate "/inventory/" pool-id "/models/" model-id "/items/create"
                                          #js {:state state
                                               :viewTransition true})

                                (if is-create
                                  (navigate (str "/inventory/" pool-id "/list?"
                                                 (params-with-all-items))
                                            #js {:state state
                                                 :viewTransition true})

                                  (navigate (str "/inventory/" pool-id "/list"
                                                 (some-> state .-searchParams))
                                            #js {:state state
                                                 :viewTransition true})))))))))]

    (uix/use-effect
     (fn []
       (when (and is-edit (not is-loading))
         (let [package (.. js/document (querySelector "[data-id='is-package']"))]
           (set! (.. package -disabled) true))))
     [is-edit is-loading])

    (if is-loading
      ($ :div {:className "flex justify-center items-center h-screen"}
         ($ Spinner))

      ($ :article
         ($ Typo {:variant :h1}
            (if is-create
              (t "pool.model.create.title")
              (t "pool.model.title")))

         ($ Typo {:variant :description
                  :class-name "mb-6"}
            (if is-create
              (t "pool.model.create.description")
              (t "pool.model.description")))

         ($ Card {:className "py-8 mb-12"}
            ($ CardContent
               ($ Scrollspy {:className "flex gap-4"}
                  ($ ScrollspyMenu {:class-name "w-1/5"})

                  ($ Form (merge form)
                     ($ :form {:id "model-form"
                               :className "space-y-12 w-full lg:w-3/5"
                               :no-validate true
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
                                                     :data data
                                                     :block block}))))))

                  ($ ButtonGroup {:class-name "ml-auto sticky self-end bottom-[1.5rem]"}
                     ($ Button {:type "submit"
                                :form "model-form"}
                        (if is-create
                          (t "pool.model.create.submit")
                          (t "pool.model.submit")))

                     ($ ButtonGroupSeparator)
                     ($ DropdownMenu
                        ($ DropdownMenuTrigger {:asChild true}
                           ($ Button {:data-test-id "submit-dropdown"
                                      :size "icon"}
                              ($ ChevronDown {:className "w-4 h-4"})))
                        ($ DropdownMenuContent {:align "end"}
                           ($ DropdownMenuItem {:asChild true}
                              ($ Button {:class-name "outline-none border-none"
                                         :variant "ghost"
                                         :type "submit"
                                         :form "model-form"
                                         :value "save-add-item"}
                                 (if is-create
                                   (t "pool.model.create.add_item")
                                   (t "pool.model.edit.add_item"))))
                           ($ DropdownMenuSeparator)
                           ($ DropdownMenuItem {:asChild true}
                              ($ Link {:to (str (router/generatePath "/inventory/:pool-id/list" params)
                                                (some-> state .-searchParams))
                                       :viewTransition true}
                                 (if is-create
                                   (t "pool.model.create.cancel")
                                   (t "pool.model.cancel"))))

                           (when (and (not is-create)
                                      (:is_deletable data))
                             ($ DropdownMenuItem {:asChild true}
                                ($ Link {:to (router/generatePath "/inventory/:pool-id/models/:model-id/delete" params)
                                         :state state}
                                   "Delete"))))))

                      ;; Dialog when deleting a model
                  (when (and (not is-create)
                             (:is_deletable data))
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
                                ($ Link {:to (router/generatePath "/inventory/:pool-id/models/:model-id" params)
                                         :state state}

                                   (t "pool.model.delete.cancel"))))))))))))))
