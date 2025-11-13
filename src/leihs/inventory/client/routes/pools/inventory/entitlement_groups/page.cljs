(ns leihs.inventory.client.routes.pools.inventory.entitlement-groups.page
  (:require
   ["@@/button" :refer [Button]]
   ["@@/card" :refer [Card CardContent CardDescription CardFooter CardHeader
                      CardTitle]]
   ["@@/table" :refer [Table TableBody TableCell TableHead TableHeader
                       TableRow]]
   ["lucide-react" :refer [SquareX SquareCheck SquareCheckBig]]
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
          ($ CardTitle (t "Anspruchsgruppen")))
       ($ CardContent
          (if (not (seq entitlement-groups))
            ($ :div {:className "flex p-6 justify-center"}
               (t "pool.entitlement-groups.list.empty"))

            ($ Table
               ($ TableHeader
                  ($ TableRow
                     ($ TableHead "")
                     ($ TableHead "")
                     ($ TableHead (t "Visierungspflichtig"))
                     ($ TableHead (t "Anzahl Benutzer"))
                     ($ TableHead (t "Anzahl Modelle"))
                     ($ TableHead (t "Anzahl Zuteilungen"))
                     ($ TableHead "")))
               ($ TableBody

                  (for [entitlement-group entitlement-groups]
                    ($ TableRow {:key (-> entitlement-group :id)}

                       ($ TableCell {:class-name (str "w-4 h-full p-0"
                                                      (if (:is_quantity_ok entitlement-group)
                                                        " bg-green-500"
                                                        " bg-red-500"))})

                       ($ TableCell {:className ""}
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
