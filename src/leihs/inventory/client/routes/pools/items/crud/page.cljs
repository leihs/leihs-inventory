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
   ["@@/card" :refer [Card CardContent]]
   ["@@/form" :refer [Form]]
   ["@@/spinner" :refer [Spinner]]
   ["@hookform/resolvers/zod" :refer [zodResolver]]
   ["lucide-react" :refer [Trash]]
   ["react-hook-form" :refer [useForm]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [Link useLoaderData]]
   ["sonner" :refer [toast]]
   [cljs.core.async :as async :refer [go <!]]
   [cljs.core.async.interop :refer-macros [<p!]]
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.pools.items.crud.components.fields :as form-fields]
   [leihs.inventory.client.routes.pools.items.crud.core :as core]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defn- on-invalid [data]
  (.. toast (error "Invalid Data"))
  (js/console.debug "is invalid: " data))

(def default-values (cj {:number-items 1
                         :inventory-code ""
                         :retired "yes"
                         :retire-reason ""
                         :working "ok"
                         :availability "ok"
                         :lendable "ok"
                         :models {:id "" :name ""}}))

(defui page []
  (let [[t] (useTranslation)
        location (router/useLocation)
        navigate (router/useNavigate)

        state (.. location -state)
        is-create (.. location -pathname (includes "create"))
        is-delete (.. location -pathname (includes "delete"))

        is-edit (not (or is-create is-delete))

        model (into {} (:model (jc (router/useLoaderData))))
        form (useForm #js {:resolver (zodResolver schema)
                           :defaultValues (if is-edit
                                            (fn [] (core/prepare-default-values model))
                                            default-values)})

        get-values (.. form -getValues)
        ;; reactive values
        is-retired (get-values "retired")
        ;; reactive values end

        is-loading (.. form -formState -isLoading)

        control (.. form -control)
        params (router/useParams)

        handle-submit (.. form -handleSubmit)
        handle-delete (fn [] (go))

        on-submit (fn [data event] (go))]

    (uix/use-effect
     (fn []
       (when (not is-loading)
         (let [retire-reason (.. js/document (querySelector "[name='retire-reason']"))]
           (if (= is-retired "yes")
             (.. retire-reason -classList (remove "hidden"))
             (.. retire-reason -classList (add "hidden"))))))
     [is-loading is-retired])

    (if is-loading
      ($ :div {:className "flex justify-center items-center h-screen"}
         ($ Spinner))

      ($ :article
         ($ :h1 {:className "text-2xl bold font-bold mt-12 mb-2"}
            (if is-create
              (t "pool.model.create.title")
              (t "pool.model.title")))

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

                                      (t "pool.model.delete.cancel")))))))))))))))


