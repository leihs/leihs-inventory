(ns leihs.inventory.client.routes.pools.inventory.list.components.collapsible-row
  (:require
   ["@@/badge" :refer [Badge]]
   ["@@/button" :refer [Button]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent
                               DropdownMenuItem DropdownMenuTrigger]]
   ["@@/table" :refer [Table TableBody TableCell TableHead TableHeader
                       TableRow]]
   ["lucide-react" :refer [Download Ellipsis Image ListRestart]]
   ["react-i18next" :refer [useTranslation]]

   ["react-router-dom" :as router :refer [Link]]

   [cljs.core.async :as async :refer [go]]
   [cljs.core.async.interop :refer-macros [<p!]]
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [model]}]
  (let [location (router/useLocation)
        [t] (useTranslation)
        params (router/useParams)

        handle-get-items (fn []
                           (go
                             (let [pool-id (aget params "pool-id")
                                   res (<p! (-> http-client
                                                (.get (str "/inventory/" pool-id "/models/" (:id model) "/items/"))
                                                (.then (fn [data]
                                                         {:status (.. data -status)
                                                          :statusText (.. data -statusText)
                                                          :data (.. data -data)}))
                                                (.catch (fn [err]
                                                          {:status (.. err -response -status)
                                                           :statusText (.. err -response -statusText)}))))
                                   status (:status res)])))]

    ($ TableRow {:key (-> model :id)
                 :style {:box-shadow "0 -0.5px 0 hsl(var(--border))"}}
       ($ TableCell
          ($ Button {:variant "outline"
                     :on-click handle-get-items
                     :size "icon"} "+"))

       ($ TableCell (-> model :total_items str))

       ($ TableCell
          (if (:url model)
            ($ :img {:class-name "w-12 h-12 object-contain"
                     :src (:url model)
                     :alt (str (:product model) " " (:version model))})
            ($ Image {:class-name "w-12 h-12"})))

       ($ TableCell
          ($ :div {:className "flex gap-2"}
             ($ Badge {:className (str "w-6 h-6 justify-center "
                                       (case (-> model :type)
                                         "Package" "bg-lime-500"
                                         "Model" "bg-slate-500"
                                         "Option" "bg-emerald-500"
                                         "Software" "bg-orange-500"))}
                (str (case (-> model :type)
                       "Package" "P"
                       "Model" "M"
                       "Option" "O"
                       "Software" "S")))))

       ($ TableCell {:className "font-bold"}
          (str (:product model) " " (:version model)))

       ($ TableCell {:className "text-right"}
          (str (-> model :in_stock str) " | " (-> model :rentable str)))

       ($ TableCell {:className "fit-content"}
          ($ :div {:className "flex gap-2"}

             ($ Button {:variant "outline"
                        :asChild true}
                ($ Link {:state #js {:searchParams (.. location -search)}
                         :to (case (-> model :type)
                               "Model" (str "../models/" (:id model))
                               "Package" (str "../models/" (:id model))
                               "Option" (str "../options/" (:id model))
                               "Software" (str "../software/" (:id model)))
                         :viewTransition true}
                   (t "pool.models.list.actions.edit")))

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
                         (t "pool.models.list.actions.add_item"))))))))))

(def CollapsibleRow
  (uix/as-react
   (fn [props]
     (main props))))
