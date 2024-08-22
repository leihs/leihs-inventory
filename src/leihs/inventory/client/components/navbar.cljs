(ns leihs.inventory.client.components.navbar
  (:require [uix.core :as uix :refer [defui $]]
            [uix.dom]
            ["lucide-react" :refer [LayoutGrid ChevronsUpDown CircleUser]]
            ["@@/input" :refer [Input]]
            ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuTrigger DropdownMenuContent DropdownMenuItem]]
            ["@@/button" :refer [Button]]))

(defui main []
  ($ :nav {:className "w-full flex flex-row text-sm items-center"}
     ($ :img {:src "/assets/zhdk-logo.svg" :className ""})
     ($ Input {:placeholder "Suche global" :className "mx-12 w-fit"})
     ($ :div {:className "flex gap-6"}
        ($ :a {:href "/verleih"} "Verleih")
        ($ :a {:href "/inventar"} "Inventar"))
     ($ DropdownMenu
        ($ DropdownMenuTrigger {:asChild "true" :className "ml-auto"}
           ($ Button {:variant "outline" :className "relative pl-10"} "Ausleihe Tonie Areal"
              ($ LayoutGrid {:className "absolute left-0 p-1 ml-2"})
              ($ ChevronsUpDown {:className "ml-2 p-1"})))

        ($ DropdownMenuContent {:className "ml-auto"}
           ($ DropdownMenuItem {:onClick #(js/alert "Profile")} "Profile")))
     ($ :div {:className "ml-12 flex items-center"} "User Name"
        ($ CircleUser {:className "ml-4"}))))
