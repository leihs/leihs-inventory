(ns leihs.inventory.client.routes
  (:require
   ["react-router-dom" :as router]
   [leihs.inventory.client.actions :as actions]
   [leihs.inventory.client.lib.utils :refer [cj]]
   [leihs.inventory.client.loader :as loader]
   [leihs.inventory.client.routes.debug.page :rename {page debug-page}]
   [leihs.inventory.client.routes.error :rename {page error-page}]
   [leihs.inventory.client.routes.layout :rename {layout root-layout}]
   [leihs.inventory.client.routes.notfound :rename {page notfound-page}]
   [leihs.inventory.client.routes.page :rename {page home-page}]
   [leihs.inventory.client.routes.pools.inventory.advanced-search.page :rename {page advanced-search-page}]
   [leihs.inventory.client.routes.pools.inventory.entitlement-groups.crud.page :rename {page entitlement-group-crud-page}]
   [leihs.inventory.client.routes.pools.inventory.entitlement-groups.page :rename {page entitlement-groups-page}]
   [leihs.inventory.client.routes.pools.inventory.layout :rename {layout inventory-layout}]
   [leihs.inventory.client.routes.pools.inventory.list.page :rename {page list-page}]
   [leihs.inventory.client.routes.pools.inventory.statistics.page :rename {page statistics-page}]
   [leihs.inventory.client.routes.pools.inventory.templates.crud.page :rename {page template-crud-page}]
   [leihs.inventory.client.routes.pools.inventory.templates.page :rename {page templates-page}]
   [leihs.inventory.client.routes.pools.items.crud.page :rename {page items-crud-page}]
   [leihs.inventory.client.routes.pools.items.review.page :rename {page items-review-page}]
   [leihs.inventory.client.routes.pools.models.crud.page :rename {page models-crud-page}]
   [leihs.inventory.client.routes.pools.options.crud.page :rename {page options-crud-page}]
   [leihs.inventory.client.routes.pools.packages.crud.page :rename {page packages-crud-page}]
   [leihs.inventory.client.routes.pools.software.crud.page :rename {page software-crud-page}]
   [uix.core :as uix :refer [$]]
   [uix.dom]))

(def routes
  (router/createBrowserRouter
   (cj
    [{:path "*"
      :id "not-found"
      :loader loader/not-found
      :element ($ :div)
      :errorElement ($ error-page)}

     {:path "/profile"
      :id "profile"
      :action actions/profile}

     {:path "/inventory"
      :id "root"
      :element ($ root-layout)
      :errorElement ($ error-page)
      :loader loader/root-layout
      :children
      (cj
       [{:index true
         :element ($ home-page)}

        {:path "debug"
         :element ($ debug-page)}

        {:path "test-error"
         :loader loader/error-test
         :element ($ :div "This should never render")}

        {:path ":pool-id"
         :children
         (cj [{:element ($ inventory-layout)
               :errorElement ($ error-page)
               :children
               (cj
                [{:index true
                  :loader #(router/redirect "list/?with_items=true&retired=false&page=1&size=50")}

                 {:path "list"
                  :loader loader/list-page
                  :action actions/list-page
                  :id "models-page"
                  :element ($ list-page)}

                 {:path "advanced-search"
                  :element ($ advanced-search-page)}

                 {:path "statistics"
                  :element ($ statistics-page)}

                 {:path "entitlement-groups"
                  :loader loader/entitlement-groups-page
                  :element ($ entitlement-groups-page)}

                 {:path "templates"
                  :loader loader/templates-page
                  :element ($ templates-page)}])}

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

              ;; entitlement group crud
              {:path "entitlement-groups/create"
               :loader loader/entitlement-group-crud-page
               :element ($ entitlement-group-crud-page)}

              {:path "entitlement-groups/:entitlement-group-id/delete?"
               :loader loader/entitlement-group-crud-page
               :element ($ entitlement-group-crud-page)}

              ;; template crud 
              {:path "templates/create"
               :loader loader/template-crud-page
               :element ($ template-crud-page)}

              {:path "templates/:template-id/delete?"
               :loader loader/template-crud-page
               :element ($ template-crud-page)}

              ;; items crud 
              {:path "items/create"
               :loader loader/items-crud-page
               :element ($ items-crud-page)}

              {:path "items/review"
               :loader loader/items-review-page
               :action actions/items-review-page
               :element ($ items-review-page)}

              {:path "items/:item-id/delete?"
               :loader loader/items-crud-page
               :element ($ items-crud-page)}

              {:path "models/:model-id/items/create"
               :loader loader/items-crud-page
               :element ($ items-crud-page)}

              ;; packages crud
              {:path "packages/create"
               :loader loader/packages-crud-page
               :element ($ packages-crud-page)}

              {:path "packages/:package-id/delete?"
               :loader loader/packages-crud-page
               :element ($ packages-crud-page)}

              {:path "models/:model-id/packages/create"
               :loader loader/packages-crud-page
               :element ($ packages-crud-page)}

              ;; Wildcard route for undefined pool routes
              {:path "*"
               :id "pool-not-found"
               :loader loader/not-found
               :element ($ :div)
               :errorElement ($ error-page)}])}

         ;; Wildcard route for undefined inventory routes
        {:path "*"
         :id "inventory-not-found"
         :loader loader/not-found
         :element ($ :div)
         :errorElement ($ error-page)}])}])))

