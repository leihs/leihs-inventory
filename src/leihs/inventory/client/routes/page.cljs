(ns leihs.inventory.client.routes.page
  (:require
   [uix.core :as uix :refer [defui $]]
   [uix.dom]))

(defui page []
  ($ :div
     ($ :h1 {:class-name "text-2xl font-bold mt-12 mb-6"} "Welcome")
     ($ :p "This is the home page with some useful links for debugging")
     ($ :ul {:class-name "mt-6"}
        ($ :li ($ :a {:class-name "underline" :href "/debug"} "Debug"))
        ($ :li ($ :a {:class-name "underline" :href "/inventory/models/inventory-list"} "Inventory List")))))

