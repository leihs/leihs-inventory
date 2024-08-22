(ns leihs.inventory.client.routes
  (:require
   ["react-router-dom" :as router :refer [BrowserRouter Routes Route
                                          createBrowserRouter RouterProvider]]
   [leihs.inventory.client.routes.debug.page :as debug]
   [leihs.inventory.client.routes.layout :as root]
   [leihs.inventory.client.routes.models.page :as models]
   [uix.core :as uix :refer [defui $]]
   [uix.dom]))

(def routes
  (createBrowserRouter
   (clj->js
    [{:element
      ($ root/layout)

      :children
      (clj->js
       [{:path "debug"
         :element ($ debug/page)}

        {:path "inventory/models/:tab"
         :element ($ models/page)}])}])))
