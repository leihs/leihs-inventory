(ns leihs.inventory.client.routes.models.page
  (:require
   ["@@/badge" :refer [Badge]]
   ["@@/button" :refer [Button]]
   ["@@/card" :refer [Card CardContent CardHeader]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent
                               DropdownMenuItem DropdownMenuLabel
                               DropdownMenuSeparator DropdownMenuTrigger]]
   ["@@/input" :refer [Input]]
   ["@@/table" :refer [Table TableBody TableCell TableHead TableHeader
                       TableRow]]
   ["lucide-react" :refer [ChevronDown Download Ellipsis Image Tags]]
   ["react-router-dom" :as router :refer [Link]]
   [leihs.inventory.client.components.pagination :as pagination]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui page [{:keys [data]}]
  (let [models (:data (router/useLoaderData))
        location (router/useLocation)
        pagination (:pagination (router/useLoaderData))]

    ($ Card {:className "my-4"}
       ($ CardHeader {:className "flex sticky top-12 bg-white rounded-md z-10"}
          ($ :div
             ($ :div {:className "flex gap-2"}
                ($ Input {:placeholder "Suche Inventar" :className "w-fit py-0"})
                ($ Button {:variant "outline"}
                   ($ Tags {:className "h-4 w-4 mr-2"}) "Inventar Typ")
                ($ Button {:variant "outline"}
                   ($ Tags {:className "h-4 w-4 mr-2"}) "Status")
                ($ Button {:variant "outline"}
                   ($ Tags {:className "h-4 w-4 mr-2"}) "Geraetepark")
                ($ Button {:variant "outline"}
                   ($ Tags {:className "h-4 w-4 mr-2"}) "Kategorien")
                ($ Button {:variant "outline"}
                   ($ Tags {:className "h-4 w-4 mr-2"}) "Inventur vor")
                ($ Button {:variant "outline" :className "ml-auto"}
                   ($ Download {:className "h-4 w-4 mr-2"}) "Export"))

             ($ :div {:className "flex gap-2 mt-2"}
                ($ DropdownMenu
                   ($ DropdownMenuTrigger {:asChild "true"}
                      ($ Button {:variant "outline"}
                         ($ :<>
                            "nicht ausgemustert "
                            ($ ChevronDown {:className "ml-2 h-4 w-4"}))))
                   ($ DropdownMenuContent {:align "start"}
                      ($ DropdownMenuItem "Inventar hinzufügen")
                      ($ DropdownMenuItem "Inventar importieren")
                      ($ DropdownMenuSeparator)
                      ($ DropdownMenuLabel "Inventar exportieren")
                      ($ DropdownMenuItem "CSV")
                      ($ DropdownMenuItem "PDF")))

                ($ DropdownMenu
                   ($ DropdownMenuTrigger {:asChild "true"}
                      ($ Button {:variant "outline"}
                         ($ :<>
                            "nur Modelle mit Gegenstanden"
                            ($ ChevronDown {:className "ml-2 h-4 w-4"}))))
                   ($ DropdownMenuContent {:align "start"}
                      ($ DropdownMenuItem "Inventar hinzufügen")
                      ($ DropdownMenuItem "Inventar importieren")
                      ($ DropdownMenuSeparator)
                      ($ DropdownMenuLabel "Inventar exportieren")
                      ($ DropdownMenuItem "CSV")
                      ($ DropdownMenuItem "PDF")))

                ($ DropdownMenu
                   ($ DropdownMenuTrigger {:asChild "true"}
                      ($ Button {:variant "outline"}
                         ($ :<>
                            "ausleihbar & nicht ausleihbar"
                            ($ ChevronDown {:className "ml-2 h-4 w-4"}))))
                   ($ DropdownMenuContent {:align "start"}
                      ($ DropdownMenuItem "Inventar hinzufügen")
                      ($ DropdownMenuItem "Inventar importieren")
                      ($ DropdownMenuSeparator)
                      ($ DropdownMenuLabel "Inventar exportieren")
                      ($ DropdownMenuItem "CSV")
                      ($ DropdownMenuItem "PDF"))))))

       ($ pagination/main {:pagination pagination
                           :class-name "justify-start p-6"})

       ($ CardContent
          ($ :section {:className "rounded-md border"}
             ($ Table
                ($ TableHeader
                   ($ TableRow
                      ($ TableHead "")
                      ($ TableHead "Menge")
                      ($ TableHead "")
                      ($ TableHead {:className "w-full"} "Name")
                      ($ TableHead "Verfügbarkeit")
                      ($ TableHead "")))
                ($ TableBody
                   (for [model models]
                     ($ TableRow {:key (-> model :id)}
                        ($ TableCell
                           ($ Button {:variant "outline"
                                      :size "icon"} "+"))

                        ($ TableCell (-> model :total str))

                        ($ TableCell
                           ($ :div {:className "flex gap-2"}
                              ($ Image)
                              ($ Badge {:className (if (= (-> model :type) "Paket")
                                                     "bg-lime-500"
                                                     "bg-slate-600")}
                                 (str (-> model :type)))))

                        ($ TableCell {:className "font-bold"}
                           (str (:product model) " " (:version model)))

                        ($ TableCell {:className "text-right"}
                           (str (-> model :available str) " | " (-> model :total str)))

                        ($ TableCell {:className "fit-content"}
                           ($ :div {:className "flex gap-2"}

                              ($ Button {:variant "outline"}
                                 ($ Link {:state #js {:searchParams (.. location -search)}
                                          :to (str (:id model))
                                          :viewTransition true}
                                    "editieren"))

                              ($ DropdownMenu
                                 ($ DropdownMenuTrigger {:asChild "true"}
                                    ($ Button {:variant "secondary"
                                               :size "icon"}
                                       ($ Ellipsis {:className "h-4 w-4"})))
                                 ($ DropdownMenuContent {:align "start"}
                                    ($ DropdownMenuItem
                                       ($ Link {:to (str (:id model) "/items/create")
                                                :state #js {:searchParams (.. location -search)}
                                                :viewTransition true}
                                          "Gegenstand erstellen"))))))))))))

       ($ pagination/main {:pagination pagination
                           :class-name "p-6 pt-0"}))))

