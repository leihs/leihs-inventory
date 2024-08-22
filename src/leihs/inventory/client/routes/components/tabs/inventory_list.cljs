(ns leihs.inventory.client.routes.components.tabs.inventory-list
  (:require
   ["lucide-react" :refer [Ellipsis Image Tags Download ChevronDown]]
   ["@@/button" :refer [Button]]
   ["@@/card" :refer [Card CardContent CardHeader]]
   ["@@/input" :refer [Input]]
   ["@@/badge" :refer [Badge]]
   ["@@/table" :refer [Table TableBody TableCell TableHead TableHeader
                       TableRow]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent DropdownMenuItem
                               DropdownMenuLabel DropdownMenuSeparator
                               DropdownMenuTrigger]]

   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui main [{:keys [data]}]
  ;; (js/console.debug data)
  ($ Card {:className "mt-4"}
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

     ($ CardContent
        ($ :section {:className "rounded-md border"}
           ($ Table
              ($ TableHeader
                 ($ TableRow
                    ($ TableHead  "")
                    ($ TableHead  "Menge")
                    ($ TableHead  "")
                    ($ TableHead {:className "w-full"} "Name")
                    ($ TableHead "Verfügbarkeit")
                    ($ TableHead "")))
              ($ TableBody
                 (for [item data]
                   ($ TableRow {:key (-> item :id)}
                      ($ TableCell
                         ($ Button {:variant "outline"
                                    :size "icon"} "+"))

                      ($ TableCell (-> item :total str))

                      ($ TableCell
                         ($ :div {:className "flex gap-2"}
                            ($ Image)
                            ($ Badge {:className (if (= (-> item :type) "Paket")
                                                   "bg-lime-500"
                                                   "bg-slate-600")}
                               (str (-> item :type)))))

                      ($ TableCell {:className "font-bold"}
                         (-> item :name str))

                      ($ TableCell {:className "text-right"}
                         (str (-> item :available str) " | " (-> item :total str)))

                      ($ TableCell {:className "fit-content"}
                         ($ :div {:className "flex gap-2"}
                            ($ Button {:variant "outline"} "editieren")
                            ($ Button {:variant "secondary"
                                       :size "icon"}
                               ($ Ellipsis {:className "h-4 w-4"}))))))))))))

