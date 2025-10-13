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
   ["~/i18n.config.js" :as i18n :refer [i18n]]
   [leihs.core.core :refer [detect]]
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.csrf :as csrf]
   [leihs.inventory.client.lib.language :refer [switch-language]]
   [leihs.inventory.client.lib.utils :refer [jc cj]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui main [{:keys [navigation available_inventory_pools user_details languages]}]
  (let [[t] (useTranslation)
        {:keys [pool-id]} (jc (router/useParams))
        fetcher (router/useFetcher)
        current-pool (->> available_inventory_pools (detect #(= pool-id (:id %))))
        current-lending-url (->> navigation :manage_nav_items (detect #(= (:name current-pool) (:name %))) :href)
        current-lang (.. i18n -language)

        #_update-lang #_(fn [event]
                          (let [new-lang (.. event -target -value)]
                            #_(-> http-client
                                  (.patch "/inventory/profile/" (cj {:language new-lang}))
                                  (.then (fn [response]
                                           (.. i18n (changeLanguage new-lang))
                                           #_(.. fetcher (load "/inventory"))))
                                  (.catch (fn [error]
                                            (js/console.error "Language change error:" error))))

                            (.. fetcher (submit (cj {:language new-lang})

                                                #js {:method "patch"
                                                     :action "/profile"
                                                     :encType "application/json"}))))]

    ($ :header {:className "bg-white sticky z-50 top-0 flex h-12 items-center gap-4 border-b h-16"}
       ($ :nav {:className "container w-full flex flex-row justify-between text-sm items-center"}
          ($ :div {:className "flex items-center"}
             ($ :img {:src "/inventory/assets/zhdk-logo.svg" :className ""})
             ($ Input {:placeholder "Suche global" :className "mx-12 w-fit"})
             ($ :div {:className "flex gap-6"}
                ($ :a {:href (or current-lending-url "/manage/")}
                   (t "header.links.lending" "Verleih"))
                ($ :a {:href "/inventory/"
                       :className "font-semibold"}
                   (t "header.links.inventory" "Inventar"))))

          ($ :div {:className "flex"}
             ($ DropdownMenu
                ($ DropdownMenuTrigger {:asChild "true" :className "ml-auto"}
                   ($ Button {:variant "outline"}
                      ($ :<>
                         ($ LayoutGrid {:className "h-4 w-4"})
                         ($ :span {:className "hidden lg:block"} (if current-pool (:name current-pool) (t "header.app-menu.inventory" "Inventar")))
                         ($ ChevronsUpDown {:className "h-4 w-4 hidden lg:block"}))))
                ($ DropdownMenuContent {:className "ml-auto"}
                   ($ DropdownMenuGroup
                      (when-let [url (:borrow_url navigation)]
                        ($ DropdownMenuItem {:asChild true}
                           ($ :a {:href url} (t "header.app-menu.borrow" "Ausleihen"))))
                      (when-let [url (:admin_url navigation)]
                        ($ DropdownMenuItem {:asChild true}
                           ($ :a {:href url} (t "header.app-menu.admin" "Admin"))))
                      (when-let [url (:procure_url navigation)]
                        ($ DropdownMenuItem {:asChild true}
                           ($ :a {:href url} (t "header.app-menu.procure" "Bedarfsermittlung")))))
                   ($ DropdownMenuSeparator)
                   ($ DropdownMenuLabel {:className "text-xs font-normal"} (t "header.app-menu.inventory-pools", "Geräteparks") ":")
                   ($ DropdownMenuGroup
                      (for [pool (sort-by :name available_inventory_pools)]
                        (let [url (router/generatePath "/inventory/:pool-id" #js {:pool-id (:id pool)})]
                          ($ DropdownMenuItem {:key (:id pool)
                                               :asChild true
                                               :className (when (= pool-id (:id pool)) "font-semibold")}
                             ($ :a {:href url} (:name pool))))))))

             ($ DropdownMenu
                ($ DropdownMenuTrigger {:asChild "true" :className "ml-4"}
                   ($ Button {:variant "outline"}
                      ($ :<>
                         ($ CircleUser {:className "h-4 w-4"})
                         ($ :span {:className "hidden lg:block"} (:name user_details))
                         ($ ChevronsUpDown {:className "h-4 w-4 hidden lg:block"}))))
                ($ DropdownMenuContent {:className "ml-auto"}
                   ($ DropdownMenuGroup
                      (when-let [url (some-> (:borrow_url navigation) (str "current-user"))]
                        ($ :<>
                           ($ DropdownMenuItem {:asChild true}
                              ($ :a {:href url} (t "header.user-menu.user-data")))
                           ($ DropdownMenuItem {:asChild true}
                              ($ :a {:href url} (t "header.user-menu.my-documents")))))
                      ($ DropdownMenuItem {:asChild true}
                         ($ :button {:type :submit
                                     :form "sign-out-form"
                                     :className "w-full"}
                            (t "header.user-menu.logout")))
                      ($ :form {:action "/sign-out"
                                :method :POST
                                :id "sign-out-form"}
                         ($ :input {:type :hidden
                                    :name csrf/token-field-name
                                    :value csrf/token})))

                   ($ DropdownMenuSeparator)
                   ($ DropdownMenuSub
                      ($ DropdownMenuSubTrigger (t "header.user-menu.language"))
                      ($ DropdownMenuPortal
                         ($ DropdownMenuSubContent
                            (for [language languages]
                              ($ DropdownMenuItem {:key (:locale language)
                                                   :asChild true}
                                 ($ fetcher.Form {:class-name "!p-0"
                                                  :method "patch"
                                                  :action "/profile"}
                                    ($ Button {:type "submit"
                                               :variant "ghost"
                                               :class-name (str "py-1.5 px-2 w-full justify-start font-normal " (when (= current-lang (:locale language)) "font-semibold"))
                                               #_:value #_(:locale lang)
                                               #_:on-click #_update-lang}
                                       (:name language))
                                    ($ Input {:type "hidden"
                                              :name "language"
                                              :value (:locale language)}))))))))))))))
