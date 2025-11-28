(ns leihs.inventory.client.routes.page
  (:require
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [Link]]
   [uix.core :as uix :refer [defui $]]
   [uix.dom]))

(defui page []
  (let [[t] (useTranslation)
        {:keys [profile]} (router/useRouteLoaderData "root")
        pools (->> (get profile :available_inventory_pools [])
                   (sort-by :name))]

    ($ :div
       ($ :h1 {:class-name "text-2xl font-bold mt-12 mb-6"} "Welcome")
       ($ :p (t "root.welcome", "Welcome to Leihs Inventory!"))
       ($ :ul {:class-name "mt-6"}
          ($ :li ($ Link {:class-name "underline" :to "debug"} "Debug"))
          ($ :li ($ :a {:class-name "underline" :href "/inventory/swagger-ui/"} "API Browser")))
       ($ :ul {:class-name "mt-6"}
          (for [pool pools]
            ($ :li {:key (:id pool)}
               ($ Link {:class-name "underline"
                        :to (str (:id pool))}
                  (:name pool))))))))

