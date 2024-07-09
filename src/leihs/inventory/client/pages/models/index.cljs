(ns leihs.inventory.client.pages.models.index
  (:require [cljs.core.async :refer [<! go]]
            [leihs.inventory.client.pages.models.api :as model-api]
            [reagent.core :as r]))

(def models (r/atom []))
(def error (r/atom nil))

(defn list-view []
  (go (let [response (<! (model-api/get-many))]
        (if (response :success)
          (do
            (reset! error nil)
            (reset! models (:body response)))
          (reset! error (str "Error " (:status response))))))
  (fn []
    [:div
     (when @error [:div [:b {:style {:color "red"}} @error]])
     (for [{:keys [id product manufacturer]} @models]
       [:div {:key id :style {:margin-bottom "10px"}}
        [:div {:style {:font-size "smaller"}} manufacturer]
        [:div product]])]))

(defn page []
  [:<>
   [:h3 "Models"]
   [list-view]])
