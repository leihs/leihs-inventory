(ns leihs.inventory.client.routes.components.header
  (:require
   ["@@/button" :refer [Button]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent
                               DropdownMenuGroup DropdownMenuItem
                               DropdownMenuLabel DropdownMenuPortal
                               DropdownMenuSeparator DropdownMenuSub
                               DropdownMenuSubContent DropdownMenuSubTrigger
                               DropdownMenuTrigger]]
   ["@@/input-group" :refer [InputGroup InputGroupAddon InputGroupInput]]
   ["lucide-react" :refer [ChevronsUpDown CircleUser LayoutGrid Search]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router" :as router]
   ["sonner" :refer [toast]]
   ["~/i18n.config.js" :as i18n :refer [i18n]]
   [clojure.string :as str]
   [leihs.core.core :refer [detect]]
   [leihs.inventory.client.lib.csrf :as csrf]
   [leihs.inventory.client.lib.hooks :as hooks]
   [leihs.inventory.client.lib.utils :refer [jc cj]]
   [leihs.inventory.client.routes.components.theme-provider :refer [use-theme]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui main [{:keys [navigation available_inventory_pools user_details languages]}]
  (let [[t] (useTranslation)
        {:keys [pool-id]} (jc (router/useParams))
        {:keys [theme set-theme]} (use-theme)
        {:keys [settings]} (router/useRouteLoaderData "root")
        current-pool (hooks/use-current-pool)
        fetcher (router/useFetcher)
        last-fetcher-data (uix/use-ref nil)
        current-lending-url (->> navigation :manage_nav_items (detect #(= (:name current-pool) (:name %))) :href)
        current-search-url (str/replace (or current-lending-url "") "/daily" "/search")
        current-lang (.. i18n -language)]

    (uix/use-effect
     (fn []
       (let [data (.-data fetcher)
             state (.-state fetcher)]
         (when (and (= state "idle")
                    (some? data)
                    (not= data @last-fetcher-data))
           (reset! last-fetcher-data data)
           (when (= (aget data "status") "error")
             (.. toast (error (t "error.action.error")
                              (clj->js {:description (t "error.action.error_detail"
                                                        #js {:httpStatus (aget data "httpStatus")})})))))))
     [fetcher t])

    ($ :header {:class-name "bg-background sticky z-50 top-0 flex items-center gap-4 border-b h-16"}
       ($ :nav {:class-name "container w-full flex flex-row justify-between text-sm items-center"}
          ($ :div {:class-name "flex items-center"}
             (let [logo-light (:logo_light settings)
                   logo-dark (:logo_dark settings)
                   resolved-theme (if (= theme "system")
                                    (if (.-matches (.matchMedia js/window "(prefers-color-scheme: dark)"))
                                      "dark"
                                      "light")
                                    theme)
                   logo-src (if (= resolved-theme "dark")
                              (or logo-dark logo-light "/inventory/assets/zhdk-logo.svg")
                              (or logo-light logo-dark "/inventory/assets/zhdk-logo.svg"))

                   logo-type (cond
                               (and (= resolved-theme "light")
                                    (or logo-light logo-dark)) "Logo light"
                               (and (= resolved-theme "dark")
                                    (or logo-dark logo-light)) "Logo dark"
                               :else "Logo default")]

               ($ :img {:src logo-src
                        :class-name "h-16 py-2"
                        :alt logo-type
                        :data-test-id "app-logo"}))
             ($ :form {:action current-search-url :method "GET"}
                ($ InputGroup {:class-name "mx-12 w-fit"}
                   ($ InputGroupInput {:name "search_term"
                                       :placeholder (t "header.links.global-search" "Suche global")})
                   ($ InputGroupAddon
                      ($ Search))))
             ($ :div {:class-name "flex gap-6"}
                ($ :a {:href (or current-lending-url "/manage/")}
                   (t "header.links.lending" "Verleih"))
                ($ :a {:href "/inventory/"
                       :class-name "font-semibold"}
                   (t "header.links.inventory" "Inventar"))))

          ($ :div {:class-name "flex"}
             ($ DropdownMenu
                ($ DropdownMenuTrigger {:asChild "true" :class-name "ml-auto"}
                   ($ Button {:variant "outline"}
                      ($ :<>
                         ($ LayoutGrid {:class-name "h-4 w-4"})
                         ($ :span {:class-name "hidden lg:block"}
                            (if current-pool (:name current-pool) (t "header.app-menu.inventory" "Inventar")))
                         ($ ChevronsUpDown {:class-name "h-4 w-4 hidden lg:block"}))))
                ($ DropdownMenuContent {:class-name "ml-auto" :data-test-id "app-menu"}
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
                   ($ DropdownMenuLabel {:class-name "text-xs font-normal"}
                      (t "header.app-menu.inventory-pools", "Geräteparks") ":")
                   ($ DropdownMenuGroup
                      (for [pool (sort-by :name available_inventory_pools)]
                        (let [url (router/generatePath "/inventory/:pool-id" #js {:pool-id (:id pool)})]
                          ($ DropdownMenuItem {:key (:id pool)
                                               :asChild true
                                               :class-name (when (= pool-id (:id pool)) "font-semibold")}
                             ($ :a {:href url} (:name pool))))))))

             ($ DropdownMenu
                ($ DropdownMenuTrigger {:asChild "true" :class-name "ml-4"}
                   ($ Button {:variant "outline"}
                      ($ :<>
                         ($ CircleUser {:class-name "h-4 w-4"})
                         ($ :span {:class-name "hidden lg:block"} (:name user_details))
                         ($ ChevronsUpDown {:class-name "h-4 w-4 hidden lg:block"}))))
                ($ DropdownMenuContent {:class-name "ml-auto"}
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
                                     :class-name "w-full"}
                            (t "header.user-menu.logout")

                            ($ :form {:action "/sign-out"
                                      :method :POST
                                      :id "sign-out-form"}
                               ($ :input {:type :hidden
                                          :name csrf/token-field-name
                                          :value csrf/token})))))

                   ($ DropdownMenuSeparator)
                   ($ DropdownMenuSub
                      ($ DropdownMenuSubTrigger
                         ($ :button {:type "button"
                                     :data-test-id "language-menu"}
                            (t "header.user-menu.language")))
                      ($ DropdownMenuPortal
                         ($ DropdownMenuSubContent
                            (for [language languages]
                              ($ DropdownMenuItem {:key (:locale language)
                                                   :asChild true}
                                 ($ :button {:type "submit"
                                             :form (str "form-" (:locale language))
                                             :data-test-id (if (= current-lang (:locale language)) "language-btn-selected" "language-btn")
                                             :class-name (str "w-full font-normal " (when (= current-lang (:locale language)) "font-semibold"))}
                                    (:name language)

                                    ($ fetcher.Form {:id (str "form-" (:locale language))
                                                     :method "PATCH"
                                                     :action "/profile"}
                                       ($ :input {:type "hidden"
                                                  :name "language"
                                                  :value (:locale language)}))))))))

                   ($ DropdownMenuSub
                      ($ DropdownMenuSubTrigger
                         ($ :button {:class-name "flex items-center gap-2"
                                     :type "button"}
                            (t "header.user-menu.theme.title")))
                      ($ DropdownMenuSubContent {:align "end"}
                         ($ DropdownMenuItem {:as-child true
                                              :onClick #(set-theme "light")}
                            ($ :button {:type "button"
                                        :class-name (str "w-full font-normal " (when (= theme "light") "font-semibold"))}
                               (t "header.user-menu.theme.light")))
                         ($ DropdownMenuItem {:as-child true
                                              :onClick #(set-theme "dark")}
                            ($ :button {:type "button"
                                        :class-name (str "w-full font-normal " (when (= theme "dark") "font-semibold"))}
                               (t "header.user-menu.theme.dark")))
                         ($ DropdownMenuItem {:as-child true
                                              :onClick #(set-theme "system")}
                            ($ :button {:type "button"
                                        :class-name (str "w-full font-normal " (when (= theme "system") "font-semibold"))}
                               (t "header.user-menu.theme.system"))))))))))))
