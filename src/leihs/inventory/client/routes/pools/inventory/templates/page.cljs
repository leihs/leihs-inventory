(ns leihs.inventory.client.routes.pools.inventory.templates.page
  (:require
   ["@@/badge" :refer [Badge]]
   ["@@/button" :refer [Button]]
   ["@@/card" :refer [Card CardContent CardHeader CardTitle CardDescription CardFooter]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent
                               DropdownMenuItem DropdownMenuTrigger]]
   ["@@/table" :refer [Table TableBody TableCell TableHead TableHeader
                       TableRow]]
   ["lucide-react" :refer [Download Ellipsis Image ListRestart Tags Tags Trash]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [Link]]
   [leihs.inventory.client.components.pagination :as pagination]
   [leihs.inventory.client.lib.utils :refer [jc]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui page []
  (let [{:keys [data]} (router/useLoaderData)
        templates (:data data)
        pagination (:pagination data)
        [t] (useTranslation)]

    ($ Card {:class-name "my-4"}
       ($ CardHeader
          ($ CardTitle (t "pool.templates.title"))
          ($ CardDescription (t "pool.templates.description")))
       ($ CardContent
          ($ :section {:className "rounded-md border overflow-x-hidden"}

             (if (not (seq templates))
               ($ :div {:className "flex p-6 justify-center"}
                  (t "pool.models.list.empty"))

               ($ Table
                  ($ TableHeader
                     ($ TableRow
                        ($ TableHead "")
                        ($ TableHead (t "pool.models.list.header.amount"))
                        ($ TableHead "")
                        ($ TableHead {:className "w-full"} (t "pool.models.list.header.name"))
                        ($ TableHead (t "pool.models.list.header.availability"))
                        ($ TableHead "")))
                  ($ TableBody

                     (for [template templates]
                       ($ TableRow {:key (-> template :id)}

                          ($ TableCell {:class-name (str "w-4 h-full p-0"
                                                         (if (:is_quantity_ok template)
                                                           " bg-green-500"
                                                           " bg-red-500"))})

                          ($ TableCell {:className "font-bold w-[80%]"}
                             (:name template))

                          ($ TableCell {:className "text-right"}
                             (str (-> template :available str) " | " (-> template :total str)))

                          ($ TableCell {:className "text-right"}
                             ($ Button {:variant "outline"}
                                "edit"))

                          ($ TableCell {:className "text-right"}
                             ($ Button {:variant "outline"
                                        :size "icon"}
                                ($ Trash {:className "h-4 w-4"}))))))))))

       ($ CardFooter {:class-name "sticky bottom-0 bg-white z-10 rounded-xl pt-6"
                      :style {:background "linear-gradient(to top, white 80%, transparent 100%)"}}
          ($ pagination/main {:pagination pagination
                              :class-name "justify-start w-full"})))))
