(ns leihs.inventory.client.routes
  (:require
   ["react-router-dom" :as router]
   [leihs.inventory.client.lib.utils :refer [cj]]
   [leihs.inventory.client.loader :as loader]
   [leihs.inventory.client.routes.advanced-search.page :rename {page advanced-search-page}]
   [leihs.inventory.client.routes.debug.page :rename {page debug-page}]
   [leihs.inventory.client.routes.entitlement-groups.page :rename {page entitlement-groups-page}]
   [leihs.inventory.client.routes.items.crud.page :rename {page items-crud-page}]
   [leihs.inventory.client.routes.layout :rename {layout root-layout}]
   [leihs.inventory.client.routes.models.crud.page :rename {page models-crud-page}]
   [leihs.inventory.client.routes.models.layout :rename {layout models-layout}]
   [leihs.inventory.client.routes.models.page :rename {page models-page}]
   [leihs.inventory.client.routes.notfound :rename {page notfound-page}]
   [leihs.inventory.client.routes.page :rename {page home-page}]
   [leihs.inventory.client.routes.statistics.page :rename {page statistics-page}]
   [uix.core :as uix :refer [$]]
   [uix.dom]))

(def routes
  (router/createBrowserRouter
   (cj
    [{:path "/inventory"
      :id "root"
      :element ($ root-layout)
      :errorElement ($ notfound-page)
      :loader loader/root-layout
      :children
      (cj
       [{:index true
         :element ($ home-page)}

        {:path "debug"
         :element ($ debug-page)}

        {:path ":pool-id"
         :children
         (cj [{:element ($ models-layout)
               :children
               (cj
                [{:index true
                  :loader #(router/redirect "models?with_items=true&retired=false&page=1&size=50")}

                 {:path "models"
                  :loader loader/models-page
                  :element ($ models-page)}

                 {:path "items"
                  :loader #(router/redirect "create")}

                 {:path "advanced-search"
                  :element ($ advanced-search-page)}

                 {:path "statistics"
                  :element ($ statistics-page)}

                 {:path "entitlement-groups"
                  :element ($ entitlement-groups-page)}])}

              ;; models crud 
              {:path "models/create"
               :loader loader/models-crud-page
               :element ($ models-crud-page)}

              {:path "models/:model-id/delete?"
               :loader loader/models-crud-page
               :element ($ models-crud-page)}

              ;; items crud 
              {:path "items/create"
               :loader loader/items-crud-page
               :element ($ items-crud-page)}

              {:path "models/:model-id/items/create"
               :loader loader/items-crud-page
               :element ($ items-crud-page)}])}])}])))
