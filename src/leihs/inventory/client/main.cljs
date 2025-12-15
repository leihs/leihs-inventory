(ns leihs.inventory.client.main
  (:require
   ["react-error-boundary" :refer [ErrorBoundary]]
   ["react-router-dom" :refer [RouterProvider ScrollRestoration]]
   [leihs.inventory.client.routes :refer [routes]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defn ^:dev/after-load start []
  (js/console.log "start"))

(defui app []
  ($ uix/strict-mode
     ($ RouterProvider {:router routes}
        ($ ErrorBoundary {:fallback (fn [^js props]
                                      (let [{:keys [error resetErrorBoundary]} (.-props props)]
                                        ($ :div
                                           ($ :h2 "Something went wrong:")
                                           ($ :pre (.-message error))
                                           ($ :button {:onClick resetErrorBoundary} "Try again"))))}

           ($ ScrollRestoration)))))

(defonce root
  (uix.dom/create-root (js/document.getElementById "app")))

(defn render []
  (uix.dom/render-root ($ app) root))

(defn ^:export init []
  (render))
