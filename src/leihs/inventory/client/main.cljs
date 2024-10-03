(ns leihs.inventory.client.main
  (:require
   ["@/i18n.js"]
   ["@tanstack/react-query" :refer [QueryClient QueryClientProvider]]
   ["react-router-dom" :refer [RouterProvider]]

   [leihs.inventory.client.routes :refer [routes]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defonce query-client (new QueryClient))

(defui app []
  ($ uix/strict-mode
     ($ QueryClientProvider {:client query-client}
        ($ RouterProvider {:router routes}))))

(defonce root
  (uix.dom/create-root (js/document.getElementById "app")))

(defn render []
  (uix.dom/render-root ($ app) root))

(defn ^:export init []
  (render))
