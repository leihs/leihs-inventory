(ns leihs.inventory.client.routes.pools.inventory.layout
  (:require
   ["@@/breadcrumb" :refer [Breadcrumb BreadcrumbItem
                            BreadcrumbLink BreadcrumbList
                            BreadcrumbSeparator BreadcrumbPage]]
   ["@@/button" :refer [Button]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent
                               DropdownMenuItem DropdownMenuTrigger]]
   ["@@/tabs" :refer [Tabs TabsContent TabsList TabsTrigger]]
   ["lucide-react" :refer [CirclePlus House]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [generatePath Link Outlet]]
   [clojure.string :as str]
   [leihs.core.core :refer [detect]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui layout []
  (let [ref (uix/use-ref nil)
        [open? set-open!] (uix/use-state false)
        pool-id (:pool-id (jc (router/useParams)))
        location (router/useLocation)
        last-segment (-> (.-pathname location)
                         (str/split #"/")
                         last)
        [t] (useTranslation)
        tabs [{:segment "list"
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
               :label (t "pool.models.tabs.entitlement_groups")}

              {:segment "templates"
               :search ""
               :label (t "pool.models.tabs.templates")}]

        {:keys [profile]} (router/useRouteLoaderData "root")
        pool (->> profile :available_inventory_pools (detect #(= (:id %) pool-id)))]

    (uix/use-effect
     (fn []
       (let [on-key-down
             (fn [e]
               (when (and (= (.. e -code) "KeyN")
                          (.-altKey e)
                          (.-shiftKey e)
                          (not (.-ctrlKey e))
                          (not (.-metaKey e)))
                 (.preventDefault e)
                 (when ref
                   (when-let [input-element (.-current ref)]
                     (.. input-element (click))))))]

         (js/window.addEventListener "keydown" on-key-down)
         (fn [] (js/window.removeEventListener "keydown" on-key-down))))
     [])

    ($ :section
       ($ Breadcrumb {:className "my-8"}
          ($ BreadcrumbList
             ($ BreadcrumbItem
                ($ BreadcrumbLink {:asChild true}
                   ($ Link {:class-name "flex items-center"
                            :to "/inventory"
                            :viewTransition true}
                      ($ House {:class-name "h-4 w-4 mr-2"})
                      (t "pool.inventory.breadcrumbs.inventory"))))

             ($ BreadcrumbSeparator)

             ($ BreadcrumbItem
                ($ BreadcrumbLink {:asChild true}
                   ($ Link {:to (generatePath "/inventory/:pool-id"
                                              (cj {:pool-id pool-id}))
                            :state #js {:searchParams (.. location -search)}
                            :viewTransition true}
                      (:name pool))))

             ($ BreadcrumbSeparator)

             ($ BreadcrumbItem
                ($ BreadcrumbPage
                   (case last-segment
                     "list" (t "pool.models.tabs.inventory_list")
                     "advanced-search" (t "pool.models.tabs.advanced_search")
                     "statistics" (t "pool.models.tabs.statistics")
                     "entitlement-groups" (t "pool.models.tabs.entitlement_groups")
                     "templates" (t "pool.models.tabs.templates"))))))

       ($ Tabs {:value last-segment}
          ($ :div {:className "flex w-full"}

             ($ TabsList
                (for [tab tabs]
                  (let [_ (str "/inventory/:pool-id/" (:segment tab) (:search tab))]
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
                (case last-segment
                  "list"
                  ($ DropdownMenu {:open open?
                                   :on-open-change set-open!}
                     ($ DropdownMenuTrigger {:asChild "true"}
                        ($ Button {:ref ref
                                   :on-click #(set-open! (not open?))}
                           ($ CirclePlus {:className "h-4 w-4"})
                           (t "pool.models.dropdown.title")))

                     ($ DropdownMenuContent {:data-test-id "add-inventory-dropdown"
                                             :align "start"}

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
                              (t "pool.models.dropdown.add_software")))))
                  "templates"
                  ($ Button {:asChild true}
                     ($ Link {:state #js {:searchParams (.. location -search)}
                              :to (generatePath "/inventory/:pool-id/templates/create"
                                                (cj {:pool-id pool-id}))
                              :viewTransition true}
                        ($ CirclePlus {:className "mr-2 h-4 w-4"})
                        (t "pool.models.add_template")))
                  "entitlement-groups"
                  ($ Button {:asChild true}
                     ($ Link {:state #js {:searchParams (.. location -search)}
                              :to (generatePath "/inventory/:pool-id/entitlement-groups/create"
                                                (cj {:pool-id pool-id}))
                              :viewTransition true}
                        ($ CirclePlus {:className "mr-2 h-4 w-4"})
                        (t "pool.models.add_entitlement_group")))
                  ($ :<>))))

          ($ TabsContent {:forceMount true
                          :tab-index -1}
             ($ Outlet))))))


