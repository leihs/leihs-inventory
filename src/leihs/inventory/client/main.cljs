(ns leihs.inventory.client.main
  (:require
   ;; ["./i18n.js" :as i18n]
   ["@tanstack/react-query" :refer [QueryClient QueryClientProvider]]
   ["react-router-dom" :refer [RouterProvider]]

   [leihs.inventory.client.routes :refer [routes]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defn ^:dev/after-load start []
  (js/console.log "start"))

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
