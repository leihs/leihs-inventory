(ns leihs.inventory.client.routes.pools.inventory.templates.page
  (:require
   ["@@/alert-dialog" :refer [AlertDialog AlertDialogAction AlertDialogCancel
                              AlertDialogContent AlertDialogDescription
                              AlertDialogFooter AlertDialogHeader
                              AlertDialogTitle]]
   ["@@/button" :refer [Button]]
   ["@@/card" :refer [Card CardContent CardDescription CardFooter CardHeader
                      CardTitle]]
   ["@@/table" :refer [Table TableBody TableCell TableHead TableHeader
                       TableRow]]
   ["lucide-react" :refer [Trash]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [Link]]
   ["sonner" :refer [toast]]
   [cljs.core.async :as async :refer [go]]
   [cljs.core.async.interop :refer-macros [<p!]]
   [leihs.inventory.client.components.pagination :as pagination]
   [leihs.inventory.client.lib.client :refer [http-client]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui page []
  (let [{:keys [data]} (router/useLoaderData)
        params (router/useParams)
        templates (:data data)
        pagination (:pagination data)
        [delete-id set-delete-id!] (uix/use-state nil)
        [t] (useTranslation)
        revalidator (router/useRevalidator)

        handle-delete (fn []
                        (go
                          (let [pool-id (aget params "pool-id")
                                res (<p! (-> http-client
                                             (.delete (str "/inventory/" pool-id "/templates/" delete-id))
                                             (.then (fn [data]
                                                      {:status (.. data -status)
                                                       :statusText (.. data -statusText)
                                                       :data (.. data -data)}))
                                             (.catch (fn [err]
                                                       {:status (.. err -response -status)
                                                        :statusText (.. err -response -statusText)}))))
                                status (:status res)]

                            (set-delete-id! nil)
                            (if (= status 200)
                              (do
                                (.. toast (success (t "pool.templates.template.delete.success")))
                                (.. revalidator (revalidate)))

                              ;; show error message)
                              (.. toast (error (t "pool.templates.template.delete.error")))))))]

    ($ Card {:class-name "my-4"}
       ($ CardHeader
          ($ CardTitle (t "pool.templates.title"))
          ($ CardDescription (t "pool.templates.description")))
       ($ CardContent
          ($ :section {:className "rounded-md border overflow-x-hidden"}

             (if (not (seq templates))
               ($ :div {:className "flex p-6 justify-center"}
                  (t "pool.templates.list.empty"))

               ($ Table
                  ($ TableHeader
                     ($ TableRow
                        ($ TableHead "")
                        ($ TableHead (t "pool.templates.list.header.name"))
                        ($ TableHead "")
                        ($ TableHead (t "pool.templates.list.header.models_count"))
                        ($ TableHead "")
                        ($ TableHead "")))
                  ($ TableBody

                     (for [template templates]
                       ($ TableRow {:key (-> template :id)}

                          ($ TableCell {:class-name (str "w-4 h-full p-0"
                                                         (if (:is_quantity_ok template)
                                                           " bg-green-500"
                                                           " bg-red-500"))})

                          ($ TableCell {:className "font-bold w-[50%]"}
                             (:name template))

                          ($ TableCell {:className "font-normal text-red-500 w-[30%] text-right"}
                             (when (not (:is_quantity_ok template))
                               (t "pool.templates.list.quantity_error")))

                          ($ TableCell {:className "w-[20%]"}
                             (:models_count template))

                          ($ TableCell {:className "text-right"}
                             ($ Button {:asChild true
                                        :variant "outline"}
                                ($ Link {:to (:id template)}
                                   (t "pool.templates.list.actions.edit"))))

                          ($ TableCell {:className "text-right"}
                             ($ Button {:variant "outline"
                                        :data-test-id "delete"
                                        :onClick #(set-delete-id! (:id template))
                                        :size "icon"}
                                ($ Trash {:className "h-4 w-4"}))))))))))

       ($ CardFooter {:class-name "sticky bottom-0 bg-white z-10 rounded-xl pt-6 overflow-auto"
                      :style {:background "linear-gradient(to top, white 80%, transparent 100%)"}}
          ($ pagination/main {:pagination pagination
                              :class-name "justify-start w-full"}))

       ($ AlertDialog {:open delete-id}
          ($ AlertDialogContent

             ($ AlertDialogHeader
                ($ AlertDialogTitle (t "pool.templates.template.delete.title"))
                ($ AlertDialogDescription (t "pool.templates.template.delete.description")))

             ($ AlertDialogFooter
                ($ AlertDialogAction {:class-name "bg-destructive text-destructive-foreground 
                                                    hover:bg-destructive hover:text-destructive-foreground"
                                      :onClick handle-delete}
                   (t "pool.templates.template.delete.confirm"))

                ($ AlertDialogCancel {:on-click #(set-delete-id! nil)}
                   (t "pool.templates.template.delete.cancel"))))))))
