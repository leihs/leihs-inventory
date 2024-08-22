(ns leihs.inventory.client.routes
  (:require
   ["react-router-dom" :as router :refer [createBrowserRouter]]
   [leihs.inventory.client.routes.debug.page :as debug]
   [leihs.inventory.client.routes.layout :as root]
   [leihs.inventory.client.routes.models.page :as models]
   [leihs.inventory.client.routes.page :as home]
   [uix.core :as uix :refer [$]]
   [uix.dom]))

(def routes
  (createBrowserRouter
   (clj->js
    [{:element
      ($ root/layout)

      :children
      (clj->js
       [{:path ""
         :element ($ home/page)}

        {:path "inventory"
         :element ($ home/page)}

        {:path "inventory/debug"
         :element ($ debug/page)}

        {:path "inventory/models/:tab"
         :element ($ models/page)}])}])))
