(ns leihs.inventory.client.routes
  (:require
   ["react-router-dom" :as router :refer [createBrowserRouter]]
   ["~/i18n.config.js" :as i18n :refer [i18n]]
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.advanced-search.page :rename {page advanced-search-page}]
   [leihs.inventory.client.routes.debug.page :rename {page debug-page}]
   [leihs.inventory.client.routes.entitlement-groups.page :rename {page entitlement-groups-page}]
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
  (createBrowserRouter
   (cj
    [{:path "/inventory"
      :id "root"
      :element
      ($ root-layout)
      :errorElement
      ($ notfound-page)
      :loader (fn []
                (-> http-client
                    (.get "/inventory/profile")
                    (.then #(jc (.. % -data)))
                    (.then #(do (.. i18n (changeLanguage (-> % :user_details :language_locale)))
                                %))))

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
                  :loader #(router/redirect "models?with_items=true&retired=false")}

                 {:path "models"
                  :element ($ models-page)
                  :loader (fn [route-data]
                            (let [url (js/URL. (.. route-data -request -url))
                                  models (-> http-client
                                             (.get (str (.-pathname url) (.-search url)))
                                             (.then #(jc (.. % -data))))]
                              models))}

                 {:path "advanced-search"
                  :element ($ advanced-search-page)}

                 {:path "statistics"
                  :element ($ statistics-page)}

                 {:path "entitlement-groups"
                  :element ($ entitlement-groups-page)}])}

              ;; models crud 
              {:path "models/create?/:model-id?/delete?"
               :loader (fn [route-data]
                         (let [params (.. ^js route-data -params)
                               path (router/generatePath "/inventory/:pool-id/entitlement-groups" params)
                               entitlement-groups (-> http-client
                                                      (.get path)
                                                      (.then #(jc (.-data %))))

                               categories (-> http-client
                                              (.get "/inventory/tree")
                                              (.then #(jc (.-data %))))

                               models (-> http-client
                                          (.get "/inventory/models-compatibles")
                                          (.then #(jc (.-data %))))

                               manufacturers (-> http-client
                                                 (.get "/inventory/manufacturers?type=Model")
                                                 (.then #(remove (fn [el] (= "" el)) (jc (.-data %)))))

                               model-path (when (:model-id (jc params)) (router/generatePath "/inventory/:pool-id/model/:model-id" params))

                               model (when model-path
                                       (-> http-client
                                           (.get model-path)
                                           (.then (fn [res]
                                                    (let [kv (first (jc (.-data res)))]
                                                      (->> kv
                                                           (vals)
                                                           (map (fn [el] (if (nil? el) "" el)))
                                                           (zipmap (keys kv))))))))]

                           (.. (js/Promise.all (cond-> [categories models manufacturers entitlement-groups]
                                                 model (conj model)))
                               (then (fn [[categories models manufacturers entitlement-groups & [model]]]
                                       {:categories categories
                                        :manufacturers manufacturers
                                        :entitlement-groups entitlement-groups
                                        :models models
                                        :model (if model model nil)})))))

               :element ($ models-crud-page)}])}])}])))
