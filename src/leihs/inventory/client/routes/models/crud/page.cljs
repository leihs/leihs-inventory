(ns leihs.inventory.client.routes.models.crud.page
  (:require
   ["@/components/react/scrollspy/scrollspy" :refer [Scrollspy ScrollspyItem
                                                     ScrollspyMenu]]
   ["@/routes/models/components/form" :refer [schema structure]]
   ["@@/alert-dialog" :refer [AlertDialog AlertDialogAction AlertDialogCancel
                              AlertDialogContent AlertDialogDescription
                              AlertDialogFooter AlertDialogHeader
                              AlertDialogTitle]]
   ["@@/button" :refer [Button]]
   ["@@/card" :refer [Card CardContent]]
   ["@@/form" :refer [Form]]
   ["@hookform/resolvers/zod" :refer [zodResolver]]
   ["lucide-react" :refer [Trash]]
   ["react-hook-form" :refer [useForm]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [Link useLoaderData]]
   ["sonner" :refer [toast]]
   [cljs.core.async :as async :refer [go]]
   [cljs.core.async.interop :refer-macros [<p!]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.models.components.forms.fields :as form-fields]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defn- on-invalid [data]
  (.. toast (error "Invalid Data"))
  (js/console.debug "is invalid: " data))

(def default-values (cj {:product ""
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
                         :accessories []}))

(defui page []
  (let [[t] (useTranslation)
        location (router/useLocation)
        navigate (router/useNavigate)
        state (.. location -state)
        is-create (.. location -pathname (includes "create"))
        is-delete (.. location -pathname (includes "delete"))
        model (into {} (:model (jc (router/useLoaderData))))

        form (useForm #js {:resolver (zodResolver schema)
                           :defaultValues (if is-create default-values (cj model))})

        control (.. form -control)
        params (router/useParams)

        handle-submit (.. form -handleSubmit)
        handle-delete (fn []
                        (go
                          (let [pool-id (aget params "pool-id")
                                model-id (aget params "model-id")
                                res (<p! (.. (js/fetch (str "/inventory/" pool-id "/model/" model-id)
                                                       (cj {:method "DELETE"
                                                            :headers {"Accept" "application/json"}}))
                                             (then #(.json %))))
                                status (.. res -status)]

                            (if (= status 200)
                              (do
                                (.. toast (success (t "pool.model.delete.success")))
                                ;; navigate to models list
                                (navigate (router/generatePath "/inventory/:pool-id/models" params)
                                          #js {:state state}))
                              ;; show error message
                              (.. toast (error (t "pool.model.delete.error")))))))

        on-submit (fn [data event]
                    (go
                      (let [images (:images (jc data))
                            attachments (:attachments (jc data))
                            model-data (into {} (remove (fn [[_ v]] (and (vector? v) (empty? v)))
                                                        (dissoc (jc data) :images :attachments)))
                            pool-id (aget params "pool-id")
                            model-res (<p! (.. (js/fetch (str "/inventory/" pool-id "/model/")
                                                         (cj {:method "POST"
                                                              :headers {"Content-Type" "application/json"
                                                                        "Accept" "application/json"}
                                                              :body (js/JSON.stringify (cj model-data))}))
                                               (then #(.json %))))
                            model-id (when (not= (.. model-res -status) "failure")
                                       (.. model-res -data -id))]

                        (.. event (preventDefault))

                        (if (.. model-res -status)
                          (.. toast (error (.. model-res -message)))

                          (do
                            ;; upload images sequentially and path model with is_cover when is needed + images with target_type
                            (doseq [image images]
                              (let [file (:file image)
                                    is-cover (:is_cover image)
                                    size (.. file -size)
                                    type (.. file -type)
                                    name (.. file -name)
                                    binary-data (<p! (.. file (arrayBuffer)))
                                    image-res (<p! (.. (js/fetch (str "/inventory/models/" model-id "/images")
                                                                 (cj {:method "POST"
                                                                      :headers {"Accept" "application/json"
                                                                                "Content-Length" size
                                                                                "Content-Type" type
                                                                                "X-Filename" name}
                                                                      :body binary-data}))
                                                       (then #(.json %))))
                                    image-id ^js (.. image-res -image -id)]

                                ;; patch image with target_type "Model"
                                #_(<p! (js/fetch (str "/inventory/" model-id "/images/" image-id)
                                                 (cj {:method "PATCH"
                                                      :headers {"Accept" "application/json"}
                                                      :body (js/JSON.stringify (cj {:target_type "Model"}))})))

                                ;; if there is a cover image then patch the model with iid of the cover image
                                (when is-cover (<p! (js/fetch (str "/inventory/" pool-id "/model/" model-id)
                                                              (cj {:method "PATCH"
                                                                   :headers {"Accept" "application/json"
                                                                             "Content-Type" "application/json"}
                                                                   :body (js/JSON.stringify (cj {:is_cover image-id}))}))))))

                            ;; upload attachments sequentially
                            (doseq [attachment attachments]
                              (let [binary-data (<p! (.. attachment (arrayBuffer)))
                                    size (.. attachment -size)
                                    type (.. attachment -type)
                                    name (.. attachment -name)]

                                (<p! (js/fetch (str "/inventory/models/" model-id "/attachments")
                                               (cj {:method "POST"
                                                    :headers {"Accept" "application/json"
                                                              "Content-Length" size
                                                              "Content-Type" type
                                                              "X-Filename" name}
                                                    :body binary-data})))))

                            (js/console.debug "is valid" data)
                            (.. toast (success (t "pool.model.create.success")))

                            ;; state needs to be forwarded for back navigation
                            (navigate (router/generatePath
                                       "/inventory/:pool-id/models/:model-id"
                                       #js {:pool-id pool-id
                                            :model-id model-id})
                                      #js {:state state}))))))]

    ($ :article
       ($ :h1 {:className "text-2xl bold font-bold mt-12 mb-2"}
          (if is-create (t "pool.model.create.title")
              (t "models.model.title")))

       ($ :h3 {:className "text-sm mb-6 text-gray-500"}
          (if is-create
            (t "pool.model.create.description")
            (t "pool.model.description")))

       ($ Card {:className "py-8 mb-12"}
          ($ CardContent
             ($ Scrollspy {:className "flex gap-4"}
                ($ ScrollspyMenu)

                ($ Form (merge form)
                   ($ :form {:id "create-model"
                             :className "space-y-12 w-full lg:w-3/5"
                             :on-submit (handle-submit on-submit on-invalid)}

                      (for [section (jc structure)]
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

                ($ :div {:className "h-max flex space-x-6 sticky bottom-0 pt-12 lg:top-[43vh] ml-auto"}

                   ($ Link {:to (str (router/generatePath "/inventory/:pool-id/models" params)
                                     (.. state -searchParams))
                            :className "self-center hover:underline"}
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
                        ($ Link {:to (router/generatePath "/inventory/:pool-id/models/:model-id/delete" params)
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
                                 ($ Link {:to (router/generatePath "/inventory/:pool-id/models/:model-id" params)
                                          :state state}

                                    (t "pool.model.delete.cancel"))))))))))))))


