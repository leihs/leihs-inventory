(ns leihs.inventory.client.routes.debug.components.lang-switcher
  (:require
   ["@@/button" :refer [Button]]
   ["@@/dropdown-menu" :refer [DropdownMenu
                               DropdownMenuContent
                               DropdownMenuItem
                               DropdownMenuLabel
                               DropdownMenuSeparator
                               DropdownMenuTrigger]]
   ["i18next" :as i18next]
   ["~/i18n.config.js" :as i18n :refer [i18n]]
   [uix.core :as uix :refer [$ defui]]))

(defui main []
  (let [languages (.. i18n -languages)]
    ($ :div {:class-name "mt-8"}
       ($ :h2 {:class-name "text-lg font-bold mb-4"} "Change Language")

       ($ DropdownMenu
          ($ DropdownMenuTrigger {:asChild true}
             ($ Button "Select Language"))
          ($ DropdownMenuContent {:data-test-id "lang-switcher-content"}
             ($ DropdownMenuLabel "Available Languages")
             ($ DropdownMenuSeparator)
             (for [language languages]
               ($ DropdownMenuItem {:key language
                                    :onClick #(.. i18next (changeLanguage language))} language)))))))
