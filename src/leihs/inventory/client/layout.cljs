(ns leihs.inventory.client.layout
  (:require
   [leihs.inventory.client.state :as state]
   [reitit.frontend.easy :as rfe]))

(defn nav []
  [:div
   [:span [:a {:href (rfe/href :home)} "Home"]]
   " | "
   [:span [:a {:href (rfe/href :models-index)} "Models"]]
   " | "
   [:span [:a {:href (rfe/href :debug-index)} "Debug"]]])

(defn app-view []
  [:div
   [nav]
   [:hr]

      (let [
            current-view (-> @state/route-match :data :view)

            p (println ">o> current-view=" current-view)
               ])

   (if-let [current-view (-> @state/route-match :data :view)]
     [current-view]
     [:div "unknown route"]
     )])
