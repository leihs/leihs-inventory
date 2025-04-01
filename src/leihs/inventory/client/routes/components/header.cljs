(ns leihs.inventory.client.routes.components.header
  (:require
   ["@@/button" :refer [Button]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent
                               DropdownMenuGroup DropdownMenuItem
                               DropdownMenuLabel DropdownMenuPortal DropdownMenuSeparator
                               DropdownMenuSub DropdownMenuSubContent DropdownMenuSubTrigger
                               DropdownMenuTrigger]]
   ["@@/input" :refer [Input]]
   ["lucide-react" :refer [ChevronsUpDown CircleUser LayoutGrid]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router]
   [leihs.core.core :refer [detect]]
   [leihs.inventory.client.lib.utils :refer [jc]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui main [{:keys [navigation available_inventory_pools user_details languages]}]
  (let [[t] (useTranslation)
        navigate (router/useNavigate)
        {:keys [pool-id]} (jc (router/useParams))
        current-pool (->> available_inventory_pools (detect #(= pool-id (:id %))))
        current-lending-url (->> navigation :manage_nav_items (detect #(= (:name current-pool) (:name %))) :href)]
    ($ :header {:className "bg-white sticky z-50 top-0 flex h-12 items-center gap-4 border-b h-16"}
       ($ :nav {:className "container w-full flex flex-row justify-between text-sm items-center"}
          ($ :div {:className "flex items-center"}
             ($ :img {:src "/inventory/assets/zhdk-logo.svg" :className ""})
             ($ Input {:placeholder "Suche global" :className "mx-12 w-fit"})
             ($ :div {:className "flex gap-6"}
                ($ :a {:href (or current-lending-url "/manage/")}
                   (t "header.links.lending" "Verleih"))
                ($ :a {:href "/inventory/" :className "font-semibold"} (t "header.links.inventory" "Inventar"))))

          ($ :div
             ($ DropdownMenu
                ($ DropdownMenuTrigger {:asChild "true" :className "ml-auto"}
                   ($ Button {:variant "outline"}
                      ($ :<>
                         ($ LayoutGrid {:className "mr-2 h-4 w-4"})
                         ($ :span {:className "hidden lg:block"} (if current-pool (:name current-pool) (t "header.app-menu.inventory" "Inventar")))
                         ($ ChevronsUpDown {:className "ml-2 h-4 w-4"}))))
                ($ DropdownMenuContent {:className "ml-auto"}
                   ($ DropdownMenuGroup
                      ($ DropdownMenuItem {:onClick #(navigate "/borrow")} (t "header.app-menu.borrow" "Ausleihen"))
                      (when-let [url (:admin_url navigation)]
                        ($ DropdownMenuItem {:onClick #(set! (.-location js/window) url)} (t "header.app-menu.admin" "Admin")))
                      (when-let [url (:procure_url navigation)]
                        ($ DropdownMenuItem {:onClick #(set! (.-location js/window) url)} (t "header.app-menu.procure" "Bedarfsermittlung"))))
                   ($ DropdownMenuSeparator)
                   ($ DropdownMenuLabel {:className "text-xs"} (t "header.app-menu.inventory-pools", "Geräteparks") ":")
                   ($ DropdownMenuGroup
                      (doall
                       (map-indexed
                        (fn [idx pool]
                          ($ DropdownMenuItem
                             {:key idx
                              :className (when (= pool-id (:id pool)) "font-semibold")
                              :onClick #(navigate (router/generatePath "/inventory/:pool-id/models" #js {:pool-id (:id pool)}))}
                             (:name pool)))
                        available_inventory_pools)))))
             ($ DropdownMenu
                ($ DropdownMenuTrigger {:asChild "true" :className "ml-4"}
                   ($ Button {:variant "outline"}
                      ($ :<>
                         ($ CircleUser {:className "mr-2 h-4 w-4"})
                         ($ :span {:className "hidden lg:block"} (:name user_details))
                         ($ ChevronsUpDown {:className "ml-2 h-4 w-4"}))))
                ($ DropdownMenuContent {:className "ml-auto"}
                   ($ DropdownMenuGroup
                      ($ DropdownMenuItem {:onClick #(js/alert "TODO")} (t "header.user-menu.user-data"))
                      ($ DropdownMenuItem {:onClick #(js/alert "TODO")} (t "header.user-menu.my-documents"))
                      ($ DropdownMenuItem {:onClick #(js/alert "TODO")} (t "header.user-menu.logout")))
                   ($ DropdownMenuSeparator)
                   ($ DropdownMenuSub
                      ($ DropdownMenuSubTrigger (t "header.user-menu.language"))
                      ($ DropdownMenuPortal
                         ($ DropdownMenuSubContent
                            (doall
                             (map-indexed
                              (fn [idx lang]
                                ($ DropdownMenuItem {:key idx :onClick #(js/alert (str "TODO - Selected lang: " (:name lang)))}
                                   (:name lang)))
                              languages))))))))))))
