(ns leihs.inventory.client.main
  (:require
   ["react-router/dom" :refer [RouterProvider]]
   [leihs.inventory.client.routes :refer [routes]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui app []
  ($ uix/strict-mode
     ($ RouterProvider {:router routes})))

(defonce root
  (uix.dom/create-root (js/document.getElementById "app")))

(defn render []
  (uix.dom/render-root ($ app) root))

#_{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
(defn ^:dev/after-load on-after-load []
  (js/console.log "after-load")
  (render))

(defn ^:export init []
  (js/console.log "init")
  (render))
