(ns leihs.inventory.client.routes
  (:require
   ["react-router-dom" :as router :refer [createBrowserRouter]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
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
               :loader (fn [route-data]
                         (let [params (.. ^js route-data -params)
                               path (router/generatePath "/inventory/:pool-id/entitlement-groups" params)
                               entitlement-groups (.. (js/fetch path
                                                                (cj {:headers {"Accept" "application/json"}}))
                                                      (then #(.json %))
                                                      (then #(jc %)))

                               categories (.. (js/fetch "/inventory/tree"
                                                        (cj {:headers {"Accept" "application/json"}}))
                                              (then #(.json %))
                                              (then #(jc %)))
                               models (.. (js/fetch "/inventory/models-compatibles"
                                                    (cj {:headers {"Accept" "application/json"}}))
                                          (then #(.json %))
                                          (then #(jc %)))
                               manufacturers (.. (js/fetch "/inventory/manufacturers?type=Model"
                                                           (cj {:headers {"Accept" "application/json"}}))
                                                 (then #(.json %))
                                                 (then #(remove (fn [el] (= "" el)) (jc %))))]

                           (.. (js/Promise.all [categories models manufacturers entitlement-groups])
                               (then (fn [[categories models manufacturers entitlement-groups]]
                                       {:categories categories
                                        :manufacturers manufacturers
                                        :entitlement-groups entitlement-groups
                                        :models models})))))
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
