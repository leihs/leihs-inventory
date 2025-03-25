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
                         :image_attributes []
                         :categories []
                         :entitlements []
                         :properties []
                         :accessories []}))

(def dev-api-origin "https://652d4449-f35d-44ea-92be-58c1e6ae0bc4.mock.pstmn.io")

(defui page []
  (let [[t] (useTranslation)
        location (router/useLocation)
        state (.. location -state)
        is-create (.. location -pathname (includes "create"))
        is-delete (.. location -pathname (includes "delete"))
        model (into {} (:model (jc (router/useLoaderData))))

        form (useForm #js {:resolver (zodResolver schema)
                           :defaultValues default-values})

        params (router/useParams)
        handleSubmit (.. form -handleSubmit)
        control (.. form -control)

        on-submit (fn [data event]
                    (go
                      (let [form-data (js/FormData.)]
                        (.. event (preventDefault))
                        (js/console.debug "is valid" (js/JSON.stringify data))

                        (.. toast (success "Data is valid"))

                        (doseq [[k v] (js/Object.entries data)]
                          (cond
                          ;; add images as binary data
                            (= k "images")
                            (if (js/Array.isArray v)
                              (doseq [val v]
                                (.. form-data (append (str "images") val)))
                              (.. form-data (append "images" v)))

                            ;; add attachments as binary data
                            (= k "attachments")
                            (if (js/Array.isArray v)
                              (doseq [val v]

                                (.. form-data (append "attachments" val)))
                              (.. form-data (append "attachments" v)))

                          ;; add fields as text data
                            :else (let [value (js/JSON.stringify v)]
                                    (.. form-data (append k value)))))

                        #_(if is-create

                            (.. (js/fetch (str dev-api-origin (router/generatePath "/inventory/:pool-id" params))
                                          (cj {:method "POST"
                                               :headers {"Accept" "application/json"}
                                               :body form-data}))
                                (then (js/console.debug "success"))
                                (catch (fn [err] (js/console.debug "error" err))))

                            (.. (js/fetch (router/generatePath "/inventory/:pool-id/model/:model-id" params)
                                          (cj {:method "PUT"
                                               :headers {"Accept" "application/json"}
                                               :body form-data}))
                                (then (js/console.debug "success"))
                                (catch (fn [err] (js/console.debug "error" err))))))))]

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
                             :on-submit (handleSubmit on-submit on-invalid)}

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
                        ($ Link {:to (router/generatePath "/inventory/:pool-id/models/:model-id/delete" params)}
                           ($ Trash {:className "w-4 h-4"}))))

                   ;; Dialog when deleting a model
                   (when (not is-create)
                     ($ AlertDialog {:open is-delete}
                        ($ AlertDialogContent

                           ($ AlertDialogHeader
                              ($ AlertDialogTitle (t "pool.model.delete.title"))
                              ($ AlertDialogDescription (t "pool.model.delete.description")))

                           ($ AlertDialogFooter
                              ($ AlertDialogAction
                                 ($ Link {:to (router/generatePath "/inventory/:pool-id/models" params)}
                                    (t "pool.model.delete.confirm")))
                              ($ AlertDialogCancel
                                 ($ Link {:to (router/generatePath "/inventory/:pool-id/models/:model-id" params)}
                                    (t "pool.model.delete.cancel"))))))))))))))


