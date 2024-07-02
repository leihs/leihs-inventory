(ns leihs.inventory.frontend.pages.debug.index
  (:require
   ["/leihs/inventory/frontend/js/elefant.js" :as elefant :refer [elefant1
                                                                  elefant2
                                                                  getAll]]
   [reitit.frontend.easy :as rfe]))

(defn page []
  [:<>
   [:h2 "Some routing tests"]
   [:div "on-click mit `navigate` " [:button {:type :button :on-click #(rfe/navigate :models-index)} "Go to models"]]
   [:div "Link mit Pfad als href " [:a {:href "/models"} "Go to models"]]
   [:div "Link mit Route als href " [:a {:href (rfe/href :models-index)} "Go to models"]]
   [:div "on-click mit `set-query` "
    [:button {:type :button :on-click #(rfe/set-query (fn [q] {:x (rand)}))} "pushState"]
    [:button {:type :button :on-click #(rfe/set-query (fn [q] {:x (rand)}) {:replace true})} "replaceState"]]

   [:h2 "JS integration tests"]
   [:div elefant/default " " elefant/elefant1 " " elefant1 " " elefant2]
   [:div "getAll: " (-> (getAll) str)]])
