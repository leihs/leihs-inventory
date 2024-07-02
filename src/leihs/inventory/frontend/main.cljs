(ns leihs.inventory.frontend.main
  (:require ["react-dom/client" :refer [createRoot]]
            [leihs.inventory.frontend.layout :as layout]
            [leihs.inventory.frontend.routing :as routing]
            [reagent.core :as r]
            [reagent.dom :as rdom]))

(def REACT18 true)
(defonce root (when REACT18 (createRoot (js/document.getElementById "app"))))
(defn render []
  (js/console.log "render root")
  (if REACT18
    (.render root (r/as-element [layout/app-view]))
    (rdom/render [layout/app-view] (js/document.getElementById "app"))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn init []
  (js/console.log "init")
  (routing/start!)
  (render))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn ^:dev/after-load after-load []
  (init))
