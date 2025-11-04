(ns leihs.inventory.client.routes
  (:require
   ["react-router-dom" :as router]
   [leihs.inventory.client.actions :as actions]
   [leihs.inventory.client.lib.utils :refer [cj]]
   [leihs.inventory.client.loader :as loader]
   [leihs.inventory.client.routes.debug.page :rename {page debug-page}]
   [leihs.inventory.client.routes.layout :rename {layout root-layout}]
   [leihs.inventory.client.routes.notfound :rename {page notfound-page}]
   [leihs.inventory.client.routes.page :rename {page home-page}]
   [leihs.inventory.client.routes.pools.inventory.advanced-search.page :rename {page advanced-search-page}]
   [leihs.inventory.client.routes.pools.inventory.entitlement-groups.page :rename {page entitlement-groups-page}]
   [leihs.inventory.client.routes.pools.inventory.layout :rename {layout inventory-layout}]
   [leihs.inventory.client.routes.pools.inventory.list.page :rename {page list-page}]
   [leihs.inventory.client.routes.pools.inventory.statistics.page :rename {page statistics-page}]
   [leihs.inventory.client.routes.pools.inventory.templates.crud.page :rename {page template-crud-page}]
   [leihs.inventory.client.routes.pools.inventory.templates.page :rename {page templates-page}]
   [leihs.inventory.client.routes.pools.items.crud.page :rename {page items-crud-page}]
   [leihs.inventory.client.routes.pools.models.crud.page :rename {page models-crud-page}]
   [leihs.inventory.client.routes.pools.options.crud.page :rename {page options-crud-page}]
   [leihs.inventory.client.routes.pools.software.crud.page :rename {page software-crud-page}]
   [uix.core :as uix :refer [$]]
   [uix.dom]))

(def routes
  (router/createBrowserRouter
   (cj
    [{:path "/profile"
      :id "profile"
      :action actions/profile}

     {:path "/inventory"
      :id "root"
      :element ($ root-layout)
      ;; :errorElement ($ notfound-page)
      :loader loader/root-layout
      :children
      (cj
       [{:index true
         :element ($ home-page)}

        {:path "debug"
         :element ($ debug-page)}

        {:path ":pool-id"
         :children
         (cj [{:element ($ inventory-layout)
               :children
               (cj
                [{:index true
                  :loader #(router/redirect "list?with_items=true&retired=false&page=1&size=50")}

                 {:path "list"
                  :loader loader/list-page
                  :id "models-page"
                  :element ($ list-page)}

                 {:path "advanced-search"
                  :element ($ advanced-search-page)}

                 {:path "statistics"
                  :element ($ statistics-page)}

                 {:path "entitlement-groups"
                  :element ($ entitlement-groups-page)}

                 {:path "templates"
                  :loader loader/templates-page
                  :element ($ templates-page)}

                 {:path "options"
                  :loader #(router/redirect "create")}

                 {:path "items"
                  :loader #(router/redirect "create")}

                 {:path "software"
                  :loader #(router/redirect "create")}])}

              ;; options crud 
              {:path "options/create"
               :loader loader/options-crud-page
               :element ($ options-crud-page)}

              {:path "options/:option-id/delete?"
               :loader loader/options-crud-page
               :element ($ options-crud-page)}

              ;; models crud 
              {:path "models/create"
               :loader loader/models-crud-page
               :element ($ models-crud-page)}

              {:path "models/:model-id/delete?"
               :loader loader/models-crud-page
               :element ($ models-crud-page)}

              ;; software crud 
              {:path "software/create"
               :loader loader/software-crud-page
               :element ($ software-crud-page)}

              {:path "software/:software-id/delete?"
               :loader loader/software-crud-page
               :element ($ software-crud-page)}

              ;; template crud 
              {:path "templates/create"
               :loader loader/template-crud-page
               :element ($ template-crud-page)}

              {:path "templates/:template-id/delete?"
               :loader loader/template-crud-page
               :element ($ template-crud-page)}

              ;; items crud 
              {:path "items/create"
               ;; :loader loader/items-crud-page
               :element ($ items-crud-page)}

              {:path "models/:item-id/items/create"
               ;; :loader loader/items-crud-page
               :element ($ items-crud-page)}])}])}])))
