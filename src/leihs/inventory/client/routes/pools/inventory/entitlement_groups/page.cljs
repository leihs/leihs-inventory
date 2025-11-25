(ns leihs.inventory.client.routes.pools.inventory.entitlement-groups.page
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
   ["lucide-react" :refer [Trash SquareCheckBig SquareX]]
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
        entitlement-groups (:data data)
        pagination (:pagination data)
        [delete-id set-delete-id!] (uix/use-state nil)
        [t] (useTranslation)
        revalidator (router/useRevalidator)

        handle-delete (fn []
                        (go
                          (let [pool-id (aget params "pool-id")
                                res (<p! (-> http-client
                                             (.delete (str "/inventory/" pool-id "/entitlement-groups/" delete-id))
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
                                (.. toast (success (t "pool.entitlement_groups.entitlement_group.delete.success")))
                                (.. revalidator (revalidate)))

                              ;; show error message)
                              (.. toast (error (t "pool.entitlement_groups.entitlement_groups.delete.error")))))))]
    ($ Card {:class-name "my-4"}
       ($ CardHeader
          ($ CardTitle (t "pool.entitlement_groups.title"))
          ($ CardDescription (t "pool.entitlement_groups.description")))
       ($ CardContent
          ($ :section {:className "rounded-md border overflow-x-hidden"}

             (if (not (seq entitlement-groups))
               ($ :div {:className "flex p-6 justify-center"}
                  (t "pool.entitlement_groups.list.empty"))

               ($ Table
                  ($ TableHeader
                     ($ TableRow
                        ($ TableHead "")
                        ($ TableHead (t "pool.entitlement_groups.list.header.name"))
                        ($ TableHead (t "pool.entitlement_groups.list.header.is_verification_required"))
                        ($ TableHead (t "pool.entitlement_groups.list.header.number_of_users"))
                        ($ TableHead (t "pool.entitlement_groups.list.header.number_of_models"))
                        ($ TableHead (t "pool.entitlement_groups.list.header.number_of_allocations"))
                        ($ TableHead "")))
                  ($ TableBody

                     (for [entitlement-group entitlement-groups]
                       ($ TableRow {:key (-> entitlement-group :id)}

                          ($ TableCell {:data-test-id "is_quantity_ok"
                                        :title (when (not (:is_quantity_ok entitlement-group))
                                                 (t "pool.entitlement_groups.list.quantity_error"))
                                        :class-name (str "w-4 h-full p-0"
                                                         (if (:is_quantity_ok entitlement-group)
                                                           " bg-green-500"
                                                           " bg-red-500"))})

                          ($ TableCell {:className "font-bold"}
                             (:name entitlement-group))

                          ($ TableCell {:data-test-id "is_verification_required"
                                        :title (str (t "pool.entitlement_groups.list.header.is_verification_required")
                                                    ": "
                                                    (if (:is_verification_required entitlement-group)
                                                      (t "pool.entitlement_groups.list.misc.yes")
                                                      (t "pool.entitlement_groups.list.misc.no")))
                                        :className "text-muted-foreground"}
                             (if (:is_verification_required entitlement-group) ($ SquareCheckBig) ($ SquareX)))

                          ($ TableCell {:data-test-id "number_of_users"}
                             (:number_of_users entitlement-group))

                          ($ TableCell {:data-test-id "number_of_models"}
                             (:number_of_models entitlement-group))

                          ($ TableCell {:data-test-id "number_of_allocations"}
                             (:number_of_allocations entitlement-group))

                          ($ TableCell {:className "text-right"}
                             ($ Button {:asChild true
                                        :variant "outline"}
                                ($ Link {:to (:id entitlement-group)}
                                   (t "pool.entitlement_groups.list.actions.edit"))))

                          ($ TableCell {:className "text-right"}
                             ($ Button {:variant "outline"
                                        :data-test-id "delete"
                                        :onClick #(set-delete-id! (:id entitlement-group))
                                        :size "icon"}
                                ($ Trash {:className "h-4 w-4"}))))))))))

       ($ CardFooter {:class-name "sticky bottom-0 bg-white z-10 rounded-xl pt-6 overflow-auto"
                      :style {:background "linear-gradient(to top, white 80%, transparent 100%)"}}
          ($ pagination/main {:pagination pagination
                              :class-name "justify-start w-full"}))

       ($ AlertDialog {:open delete-id}
          ($ AlertDialogContent

             ($ AlertDialogHeader
                ($ AlertDialogTitle (t "pool.entitlement_groups.entitlement_group.delete.title"))
                ($ AlertDialogDescription (t "pool.entitlement_groups.entitlement_group.delete.description")))

             ($ AlertDialogFooter
                ($ AlertDialogAction {:class-name "bg-destructive text-destructive-foreground 
                                                   hover:bg-destructive hover:text-destructive-foreground"
                                      :onClick handle-delete}
                   (t "pool.entitlement_groups.entitlement_group.delete.confirm"))

                ($ AlertDialogCancel {:on-click #(set-delete-id! nil)}
                   (t "pool.entitlement_groups.entitlement_group.delete.cancel"))))))))
