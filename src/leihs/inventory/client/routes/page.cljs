(ns leihs.inventory.client.routes.page
  (:require
   ["react-i18next" :refer [useTranslation]]
   [uix.core :as uix :refer [defui $]]
   [uix.dom]))

(defui page []
  (let [[t] (useTranslation)]

    ($ :div
       ($ :h1 {:class-name "text-2xl font-bold mt-12 mb-6"} "Welcome")
       ($ :p (t "root.welcome", "Welcome to Leihs Inventory!"))
       ($ :ul {:class-name "mt-6"}
          ($ :li ($ :a {:class-name "underline" :href "/inventory/debug"} "Debug"))
          ($ :li ($ :a {:class-name "underline" :href "/inventory/models"} "Inventory List"))))))

