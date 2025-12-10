(ns leihs.inventory.client.routes.pools.inventory.entitlement-groups.page
  (:require
   ["@@/button" :refer [Button]]
   ["@@/card" :refer [Card CardContent CardDescription CardHeader
                      CardTitle]]
   ["@@/table" :refer [Table TableBody TableCell TableHead TableHeader
                       TableRow]]
   ["lucide-react" :refer [SquareX SquareCheckBig]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [Link]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui page []
  (let [data (router/useLoaderData)
        entitlement-groups (:data data)
        [t] (useTranslation)]
    ($ Card {:class-name "my-4"}
       ($ CardHeader
          ($ CardTitle (t "pool.entitlement-groups.title"))
          ($ CardDescription (t "pool.entitlement-groups.description")))
       ($ CardContent
          (if (not (seq entitlement-groups))
            ($ :div {:className "flex p-6 justify-center"}
               (t "pool.entitlement-groups.list.empty"))

            ($ Table
               ($ TableHeader
                  ($ TableRow
                     ($ TableHead "")
                     ($ TableHead (t "pool.entitlement-groups.list.header.name"))
                     ($ TableHead (t "pool.entitlement-groups.list.header.is_verification_required"))
                     ($ TableHead (t "pool.entitlement-groups.list.header.number_of_users"))
                     ($ TableHead (t "pool.entitlement-groups.list.header.number_of_models"))
                     ($ TableHead (t "pool.entitlement-groups.list.header.number_of_allocations"))
                     ($ TableHead "")))
               ($ TableBody

                  (for [entitlement-group entitlement-groups]
                    ($ TableRow {:key (-> entitlement-group :id)}

                       ($ TableCell {:class-name (str "w-4 h-full p-0"
                                                      (if (:is_quantity_ok entitlement-group)
                                                        " bg-green-500"
                                                        " bg-red-500"))})

                       ($ TableCell {:className "font-bold"}
                          (:name entitlement-group))

                       ($ TableCell {:className "text-muted-foreground"}
                          (if (:is_verification_required entitlement-group) ($ SquareCheckBig) ($ SquareX)))

                       ($ TableCell {:className ""}
                          (:number_of_users entitlement-group))

                       ($ TableCell {:className ""}
                          (:number_of_models entitlement-group))

                       ($ TableCell {:className ""}
                          "...")

                       ($ TableCell {:className "text-right"}
                          ($ Button {:asChild true
                                     :variant "outline"}
                             ($ Link {:to (:id entitlement-group)}
                                (t "pool.entitlement-groups.list.actions.edit")))))))))))))
