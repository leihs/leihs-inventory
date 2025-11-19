(ns leihs.inventory.client.routes.pools.inventory.list.components.inventory-pool-filter
  (:require
   ["@/components/ui/command" :refer [Command CommandEmpty CommandGroup
                                      CommandInput CommandItem CommandList]]
   ["@/components/ui/popover" :refer [Popover PopoverContent PopoverTrigger]]
   ["@@/button" :refer [Button]]
   ["lucide-react" :refer [Check ChevronsUpDown Building]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [class-name]}]
  (let [inventory-pools (:responsible-pools (router/useRouteLoaderData "models-page"))
        [search-params set-search-params!] (router/useSearchParams)
        buttonRef (uix/use-ref nil)
        [open set-open!] (uix/use-state false)
        type (.. search-params (get "type"))
        [t] (useTranslation)

        pool-id (.get search-params "inventory_pool_id")
        handle-select (fn [id]
                        (if (= pool-id id)
                          (.delete search-params "inventory_pool_id")
                          (.set search-params "inventory_pool_id" id))

                        (.set search-params "page" "1")
                        (set-search-params! search-params)
                        (set-open! false))]

    ($ Popover {:open open
                :on-open-change #(set-open! %)}
       ($ PopoverTrigger {:as-child true}
          ($ Button {:ref buttonRef
                     :disabled (= type "option")
                     :on-click #(set-open! (not open))
                     :class-name class-name
                     :variant "outline"
                     :role "combobox"}
             ($ Building {:className "h-4 w-4 mr-2"})
             (t "pool.models.filters.inventory_pool.title")
             ($ ChevronsUpDown {:class-name "ml-2 h-4 w-4 shrink-0 opacity-50"})))

       ($ PopoverContent {:align "start"
                          :class-name "p-0"}
          ($ Command
             ($ CommandInput {:placeholder (t "pool.models.filters.inventory_pool.placeholder")})
             ($ CommandList
                ($ CommandEmpty (t "pool.models.filters.inventory_pool.not_found"))
                ($ CommandGroup
                   (for [inventory-pool inventory-pools]
                     ($ CommandItem {:value (:id inventory-pool)
                                     :keywords #js [(:name inventory-pool)]
                                     :onSelect handle-select
                                     :key (:id inventory-pool)}

                        ($ Check
                           {:class-name (str "mr-2 h-4 w-4 "
                                             (if (= (:id inventory-pool) pool-id)
                                               "visible"
                                               "invisible"))})
                        (:name inventory-pool))))))))))

(def InventoryPoolFilter
  (uix/as-react
   (fn [props]
     (main props))))
