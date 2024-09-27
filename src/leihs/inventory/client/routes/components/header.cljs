(ns leihs.inventory.client.routes.components.header
  (:require ["@@/button" :refer [Button]]
            ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuTrigger DropdownMenuContent DropdownMenuItem]]
            ["@@/input" :refer [Input]]
            ["lucide-react" :refer [LayoutGrid ChevronsUpDown CircleUser]]
            ["react-i18next" :refer [useTranslation]]
            [uix.core :as uix :refer [defui $]]
            [uix.dom]))

(defui main []
  (let [[t] (useTranslation)]

    ($ :header {:className "bg-white sticky z-50 top-0 flex h-12 items-center gap-4 px-4 md:px-6 border-b h-16"}
       ($ :nav {:className "container w-full flex flex-row text-sm items-center"}
          ($ :img {:src "/inventory/static/zhdk-logo.svg" :className ""})
          ($ Input {:placeholder "Suche global" :className "mx-12 w-fit"})
          ($ :div {:className "flex gap-6"}
             ($ :a {:href "/lending"}
                (t "header.links.lending" "Ausleihe"))

             ($ :a {:href "/inventory/models"} (t "header.links.inventory" "Inventar")))
          ($ DropdownMenu
             ($ DropdownMenuTrigger {:asChild "true" :className "ml-auto"}
                ($ Button {:variant "outline"}
                   ($ :<>
                      ($ LayoutGrid {:className "mr-2 h-4 w-4"})
                      ($ :<> "Ausleihe Tonie Areal")
                      ($ ChevronsUpDown {:className "ml-2 h-4 w-4"}))))

             ($ DropdownMenuContent {:className "ml-auto"}
                ($ DropdownMenuItem {:onClick #(js/alert "Profile")} "Profile")))
          ($ :div {:className "ml-12 flex items-center"} "User Name"
             ($ CircleUser {:className "ml-4"}))))))
