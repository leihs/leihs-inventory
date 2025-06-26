(ns leihs.inventory.client.routes.models.layout
  (:require
   ["@@/button" :refer [Button]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent
                               DropdownMenuItem DropdownMenuTrigger]]
   ["@@/tabs" :refer [Tabs TabsContent TabsList TabsTrigger]]
   ["lucide-react" :refer [CirclePlus]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [generatePath Link Outlet]]
   [clojure.string :as str]
   [leihs.core.core :refer [detect]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui layout []
  (let [pool-id (:pool-id (jc (router/useParams)))
        location (router/useLocation)
        last-segment (-> (.-pathname location)
                         (str/split #"/")
                         last)
        [t] (useTranslation)
        tabs [{:segment "models"
               :search "?with_items=true&page=1&size=50"
               :label (t "pool.models.tabs.inventory_list")}

              {:segment "advanced-search"
               :search ""
               :label (t "pool.models.tabs.advanced_search")}

              {:segment "statistics"
               :search ""
               :label (t "pool.models.tabs.statistics")}

              {:segment "entitlement-groups"
               :search ""
               :label (t "pool.models.tabs.entitlement_groups")}]
        profile (router/useRouteLoaderData "root")
        pool (->> profile :available_inventory_pools (detect #(= (:id %) pool-id)))]

    ($ :article
       ($ :h1 {:className "text-2xl font-bold mt-12 mb-6"}
          (t "pool.models.title") " - " (:name pool))

       ($ Tabs {:value last-segment}
          ($ :div {:className "flex w-full"}

             ($ TabsList
                (for [tab tabs]
                  (let [path (str "/inventory/:pool-id/" (:segment tab) (:search tab))]
                    ($ TabsTrigger
                       {:key (:segment tab)
                        :asChild true
                        :value (:segment tab)}
                       ($ Link
                          {:to (str (:segment tab) (:search tab))
                           :state #js {:searchParams (.. location -search)}
                           :viewTransition true}
                          (:label tab))))))

             ($ :div {:className "ml-auto"}
                ($ DropdownMenu
                   ($ DropdownMenuTrigger {:asChild "true"}
                      ($ Button
                         ($ CirclePlus {:className "mr-2 h-4 w-4"})
                         (t "pool.models.dropdown.title")))

                   ($ DropdownMenuContent {:align "start"}

                      ($ DropdownMenuItem {:asChild true}
                         ($ Link {:state #js {:searchParams (.. location -search)}
                                  :to (generatePath "/inventory/:pool-id/models/create"
                                                    (cj {:pool-id pool-id}))
                                  :viewTransition true}
                            (t "pool.models.dropdown.add_model")))

                      ($ DropdownMenuItem {:asChild true}
                         ($ Link {:state #js {:searchParams (.. location -search)}
                                  :to (generatePath "/inventory/:pool-id/items/create"
                                                    (cj {:pool-id pool-id}))
                                  :viewTransition true}
                            (t "pool.models.dropdown.add_item")))

                      ($ DropdownMenuItem {:asChild true}
                         ($ Link {:state #js {:searchParams (.. location -search)}
                                  :to (generatePath "/inventory/:pool-id/options/create"
                                                    (cj {:pool-id pool-id}))
                                  :viewTransition true}
                            (t "pool.models.dropdown.add_option")))

                      ($ DropdownMenuItem {:asChild true}
                         ($ Link {:state #js {:searchParams (.. location -search)}
                                  :to (generatePath "/inventory/:pool-id/software/create"
                                                    (cj {:pool-id pool-id}))
                                  :viewTransition true}
                            (t "pool.models.dropdown.add_software")))))))

          ($ TabsContent {:forceMount true}
             ($ Outlet))))))


