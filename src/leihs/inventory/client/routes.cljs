(ns leihs.inventory.client.routes
  (:require
   ["react-router-dom" :as router :refer [createBrowserRouter]]
   [leihs.inventory.client.lib.utils :refer [cj]]
   [leihs.inventory.client.routes.debug.page :rename {page debug-page}]
   [leihs.inventory.client.routes.layout :rename {layout root-layout}]
   [leihs.inventory.client.routes.models.advanced-search.page :rename {page advanced-search-page}]
   [leihs.inventory.client.routes.models.create.page :rename {page models-create-page}]
   [leihs.inventory.client.routes.models.entitlement-groups.page :rename {page entitlement-groups-page}]
   [leihs.inventory.client.routes.models.layout :rename {layout models-layout}]
   [leihs.inventory.client.routes.models.page :rename {page models-page}]
   [leihs.inventory.client.routes.models.statistics.page :rename {page statistics-page}]
   [leihs.inventory.client.routes.notfound :rename {page notfound-page}]
   [leihs.inventory.client.routes.page :rename {page home-page}]
   [uix.core :as uix :refer [$]]
   [uix.dom]))

(def routes
  (createBrowserRouter
   (cj
    [{:path "/inventory"
      :element
      ($ root-layout)
      :errorElement
      ($ notfound-page)

      :children
      (cj
       [{:index true
         :element ($ home-page)}

        {:path "debug"
         :element ($ debug-page)}

        {:path ":pool-id"
         :children
         (cj [{:path "models/create"
               :element ($ models-create-page)}

              {:path "models/edit"
               :element ($ models-create-page)}

              {:element ($ models-layout)
               :children
               (cj
                [{:path "models"
                  :element ($ models-page)}

                 {:path "advanced-search"
                  :element ($ advanced-search-page)}

                 {:path "statistics"
                  :element ($ statistics-page)}

                 {:path "entitlement-groups"
                  :element ($ entitlement-groups-page)}])}])}])}])))
