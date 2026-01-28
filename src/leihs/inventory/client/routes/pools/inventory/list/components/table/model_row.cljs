(ns leihs.inventory.client.routes.pools.inventory.list.components.table.model-row
  (:require
   ["@@/badge" :refer [Badge]]
   ["@@/button" :refer [Button]]
   ["@@/button-group" :refer [ButtonGroup]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent
                               DropdownMenuItem DropdownMenuTrigger]]
   ["@@/table" :refer [TableCell]]
   ["@@/tooltip" :refer [Tooltip TooltipTrigger TooltipContent]]
   ["lucide-react" :refer [Ellipsis Columns3Cog Image ChevronDown SquareMenu]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [Link]]
   ["sonner" :refer [toast]]
   [clojure.string :as str]
   [leihs.inventory.client.components.image-modal :refer [ImageModal]]
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.pools.inventory.list.components.table.expandable-row :refer [ExpandableRow]]
   [leihs.inventory.client.routes.pools.inventory.list.components.table.item-row :refer [ItemRow]]
   [leihs.inventory.client.routes.pools.inventory.list.components.table.package-row :refer [PackageRow]]
   [uix.core :as uix :refer [$ defui]]))

(def query-keys [:owned :incomplete
                 :broken :retired :borrowable
                 :fields :in_stock
                 :model_id :parent_id :inventory_pool_id :search])

(def fields ["id" "is_package" "is_borrowable" "is_broken" "retired" "is_incomplete"
             "price" "inventory_code" "shelf" "building_code" "package_items"
             "building_name" "reservation_end_date" "shelf" "inventory_pool_name"
             "user_name" "reservation_user_name" "url" "reservation_contract_id"])

(defui main [{:keys [model className]}]
  (let [location (router/useLocation)
        {:keys [settings]} (router/useRouteLoaderData "root")
        [t] (useTranslation)
        params (router/useParams)
        pool-id (aget params "pool-id")
        [search-params _] (router/useSearchParams)

        [result set-result!] (uix/use-state nil)
        handle-expand (fn []
                        (let [param-map (into {}
                                              (for [[key val] (.entries search-params)]
                                                [(keyword key) (str val)]))
                              params (merge {:model_id (:id model)
                                             :fields (str/join "," fields)}
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
                                          (.. toast (error (.. err -response -status)
                                                           #js {:description (.. err -response -statusText)}))))))))]

    (uix/use-effect
     (fn []
       (set-result! nil))
     [search-params])

    ($ ExpandableRow {:key (-> model :id)
                      :data-row "model"
                      :subrow-count (:items model)
                      :class-name (str
                                   "[&+tr[data-row='item']]:shadow-[0_-0.5px_0_hsl(var(--shadow)),inset_0_4px_4px_-2px_hsl(var(--shadow))] "
                                   "[&+tr[data-row='package']]:shadow-[0_-0.5px_0_hsl(var(--shadow)),inset_0_4px_4px_-2px_hsl(var(--shadow))] "
                                   (if result "bg-secondary/75 " " ")
                                   className)
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
            ($ ImageModal {:url (:url model)
                           :alt (str (:product model) " " (:version model))})

            (case (-> model :type)
              "Option" ($ Columns3Cog {:class-name "w-12 h-12"})
              ($ Image {:class-name "w-12 h-12"}))))

       ($ TableCell
          ($ :div {:className "flex gap-2"}
             ($ Badge {:className (str "w-6 h-5 justify-center shadow-none "
                                       (case (-> model :type)
                                         "Package" "bg-lime-500"
                                         "Model" "bg-slate-500"
                                         "Option" "bg-emerald-500"
                                         "Software" "bg-orange-500"))
                       :data-test-id "type"}
                (str (case (-> model :type)
                       "Package" "P"
                       "Model" "M"
                       "Option" "O"
                       "Software" "S")))))

       ($ TableCell {:className "font-bold"}
          (str (:product model) " " (:version model)))

       ($ TableCell {:className "text-right"}
          (case (:type model)
            "Option"
            ($ :span {:data-test-id "price"}
               (str (or (:price model) "0") " " (:local_currency_string settings)))

            ($ Tooltip
               ($ TooltipTrigger {:asChild true}
                  ($ :span {:data-test-id "availability"}
                     (str (-> model :in_stock str) " | " (-> model :rentable str))))
               ($ TooltipContent
                  ($ :<>
                     ($ :div (str (t "pool.models.list.tooltip.in_stock")
                                  ": " (-> model :in_stock str)))
                     ($ :div (str (t "pool.models.list.tooltip.rentable")
                                  ": " (-> model :rentable str))))))))

       ($ TableCell {:className "fit-content"}
          ($ ButtonGroup
             ($ Button {:variant "outline"
                        :class-name ""
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
                ($ DropdownMenuTrigger {:asChild true}
                   ($ Button {:data-test-id "edit-dropdown"
                              :class-name ""
                              :variant "outline"
                              :size "icon"}
                      ($ ChevronDown {:className "w-4 h-4"})))
                ($ DropdownMenuContent {:align "start"}
                   ($ DropdownMenuItem
                      ($ Link {:to (str "../models/" (:id model) "/items/create")
                               :state #js {:searchParams (.. location -search)}
                               :viewTransition true}
                         (t "pool.models.list.actions.add_item"))))))))))

(def ModelRow
  (uix/as-react
   (fn [props]
     (main props))))
