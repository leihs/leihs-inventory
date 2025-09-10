(ns leihs.inventory.client.routes.pools.inventory.list.components.table.model-row
  (:require
   ["@@/badge" :refer [Badge]]
   ["@@/button" :refer [Button]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent
                               DropdownMenuItem DropdownMenuTrigger]]
   ["@@/table" :refer [TableCell]]
   ["lucide-react" :refer [Ellipsis Image]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [Link]]
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.pools.inventory.list.components.table.expandable-row :refer [ExpandableRow]]
   [leihs.inventory.client.routes.pools.inventory.list.components.table.item-row :refer [ItemRow]]
   [leihs.inventory.client.routes.pools.inventory.list.components.table.package-row :refer [PackageRow]]
   [uix.core :as uix :refer [$ defui]]))

(def query-keys [:owned :incomplete
                 :broken :retired :borrowable
                 :fields :in_stock
                 :model_id :parent_id])

(defui main [{:keys [model]}]
  (let [location (router/useLocation)
        [t] (useTranslation)
        params (router/useParams)
        pool-id (aget params "pool-id")
        [search-params _] (router/useSearchParams)

        [result set-result!] (uix/use-state nil)
        handle-expand (fn []
                        (let [param-map (into {}
                                              (for [[key val] (.entries search-params)]
                                                [(keyword key) (str val)]))
                              params (merge {:model_id (:id model)}
                                            (select-keys param-map query-keys))]

                          (if result
                            (set-result! nil)

                            (-> http-client
                                (.get (str "/inventory/" pool-id "/items/")
                                      (cj {:cache false
                                           :params (router/createSearchParams (cj params))}))
                                (.then (fn [data]
                                         (set-result! {:status (.. data -status)
                                                       :statusText (.. data -statusText)
                                                       :data (jc (.. data -data))})))
                                (.catch (fn [err]
                                          {:status (.. err -response -status)
                                           :statusText (.. err -response -statusText)}))))))]

    (uix/use-effect
     (fn []
       (set-result! nil))
     [search-params])

    ($ ExpandableRow {:key (-> model :id)
                      :subrow-count (:total_items model)
                      :class-name (if result "bg-accent/70 hover:bg-accent/70" "")
                      :on-expand handle-expand
                      :subrows (when (and result (= (:status result) 200))
                                 (when (seq (:data result))
                                   (map
                                    (fn [element]
                                      (if (not (:is_package element))
                                        ($ ItemRow {:key (:id element)
                                                    :item element})
                                        ($ PackageRow {:key (:id element)
                                                       :package element})))
                                    (:data result))))}

       ($ TableCell
          (if (:url model)
            ($ :img {:class-name "min-w-12 h-12 object-contain rounded border p-1"
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

(def ModelRow
  (uix/as-react
   (fn [props]
     (main props))))
