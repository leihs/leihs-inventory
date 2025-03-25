(ns leihs.inventory.client.main
  (:require
   ;; ["./i18n.js" :as i18n]
   ["react-router-dom" :refer [RouterProvider]]

   [leihs.inventory.client.routes :refer [routes]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defn ^:dev/after-load start []
  (js/console.log "start"))

(defui app []
  ($ uix/strict-mode
     ($ RouterProvider {:router routes})))

(defonce root
  (uix.dom/create-root (js/document.getElementById "app")))

(defn render []
  (uix.dom/render-root ($ app) root))

(defn ^:export init []
  (render))
