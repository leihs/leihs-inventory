(ns leihs.inventory.client.routes.models.layout
  (:require
   ["@@/button" :refer [Button]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent
                               DropdownMenuItem DropdownMenuTrigger]]
   ["@@/tabs" :refer [Tabs TabsContent TabsList TabsTrigger]]
   ["lucide-react" :refer [CirclePlus]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [generatePath Link Outlet useParams
                                          useResolvedPath]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(def tabs [{:segment "models" :label "Inventarliste"}
           {:segment "advanced-search" :label "Erweiterte Suche"}
           {:segment "statistics" :label "Statistik"}
           {:segment "entitlement-groups" :label "Anspruchsgruppen"}])

(defui layout []
  (let [tab-route (jc (router/useResolvedPath))
        pool-id (:pool-id (jc (router/useParams)))
        location (router/useLocation)
        [t] (useTranslation)]

    ($ :article
       ($ :h1 {:className "text-2xl font-bold mt-12 mb-6"}
          (t "models.title", "Inventarliste - Ausleihe Toni Areal Localized"))

       ($ Tabs {:value (:pathname tab-route)}
          ($ :div {:className "flex w-full"}

             ($ TabsList
                (for [tab tabs]
                  (let [path (str "/inventory/:pool-id/" (:segment tab))]
                    ($ TabsTrigger
                       {:key (:segment tab)
                        :asChild true
                        :value (generatePath path (cj {:pool-id pool-id}))}
                       ($ Link
                          {:to (:segment tab)}
                          (:label tab))))))

             ($ :div {:className "ml-auto"}
                ($ DropdownMenu
                   ($ DropdownMenuTrigger {:asChild "true"}
                      ($ Button
                         ($ CirclePlus {:className "mr-2 h-4 w-4"})
                         "Inventar Hinzufügen"))

                   ($ DropdownMenuContent {:align "start"}

                      ($ DropdownMenuItem {:asChild true}
                         ($ Link {:state #js {:searchParams (.. location -search)}
                                  :to (generatePath "/inventory/:pool-id/models/create"
                                                    (cj {:pool-id pool-id}))}
                            "Neues Modell"))

                      ($ DropdownMenuItem {:asChild true}
                         ($ Link {:to (generatePath "/inventory/:pool-id/items/create"
                                                    (cj {:pool-id pool-id}))}
                            "Neuer Gegenstand"))))))

          ($ TabsContent {:forceMount true}
             ($ Outlet))))))


