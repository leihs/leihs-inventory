(ns leihs.inventory.client.pages.models.index
  (:require [cljs.core.async :refer [<! go]]
            [leihs.inventory.client.pages.models.api :as model-api]
            [reagent.core :as r]))

(def models (r/atom []))
(def error (r/atom nil))
(def editing (r/atom false))
(def current-model (r/atom nil))

(defn start-editing [model]
  (reset! editing true)
  (reset! current-model model))

(defn stop-editing []
  (reset! editing false))

(defn save-changes [id new-model]
  (edit-model id new-model)
  (stop-editing))

;(defn edit-model [id new-model]
;  ;; Logic for editing a model goes here
;  (go (let [response (<! (model-api/edit id new-model))]
;        (if (:success response)
;          (do
;            (println "Edit model" id)
;            (fetch-models))
;          (reset! error (str "Error " (:status response))))))

(defn delete-model [id]
  ;; Logic for deleting a model goes here
  (go (let [response (<! (model-api/delete id))]
        (if (:success response)
          (do
            (println "Delete model" id)
            (fetch-models))
          (reset! error (str "Error " (:status response)))))))

  (defn add-model [model]
    ;; Logic for adding a model goes here
    (go (let [response (<! (model-api/add model))]
          (if (:success response)
            (do
              (println "Add model" model)
              (fetch-models))
            (reset! error (str "Error " (:status response)))))))

;  (defn list-view []
;    (fn []
;      [:div
;       (when @error [:div [:b {:style {:color "red"}} @error]])
;       ;(for [{:keys [id product manufacturer]} @models]
;       (for [{:keys [product manufacturer]} @models]
;         (do
;           ;(println ">o> row => " id product manufacturer)
;           (if (and @editing (= id (:id @current-model)))
;             [:div
;              [:input {:type "text" :value id :on-change #(swap! current-model assoc :id (-> % .-target .-value))}]
;              [:input {:type "text" :value product :on-change #(swap! current-model assoc :product (-> % .-target .-value))}]
;              [:input {:type "text" :value manufacturer :on-change #(swap! current-model assoc :manufacturer (-> % .-target .-value))}]
;              ;[:button {:on-click #(save-changes id @current-model)} "Save"]
;              ;[:button {:on-click stop-editing} "Cancel"]
;               ]
;             [:div
;              {:key id :style {:margin-bottom "10px"}}
;              [:div {:style {:font-size "smaller"}} manufacturer]
;              [:div product]
;              ;[:button {:on-click #(start-editing {:id id :product product :manufacturer manufacturer})} "Edit"]
;              ;[:button {:on-click #(delete-model id)} "Delete"]
;              ])))])))
;
;(defn list-view []
;  (go (let [response (<! (model-api/get-many))]
;        (if (response :success)
;          (do
;            (reset! error nil)
;            (reset! models (:body response)))
;          (reset! error (str "Error " (:status response))))))
;  (fn []
;    [:div
;     (when @error [:div [:b {:style {:color "red"}} @error]])
;     (for [{:keys [id product manufacturer]} @models]
;       [:div {:key id :style {:margin-bottom "10px"}}
;        [:div id]
;        [:div {:style {:font-size "smaller"}} manufacturer]
;        [:div product]
;        ])]))
;
;
;(defn list-view []
;  (go (let [response (<! (model-api/get-many))]
;        (if (response :success)
;          (do
;            (reset! error nil)
;            (reset! models (:body response)))
;          (reset! error (str "Error " (:status response))))))
;  (fn []
;    [:div
;     (when @error [:div [:b {:style {:color "red"}} @error]])
;     [:table
;      [:tr [:th "ID"] [:th "Product"] [:th "Manufacturer"]]
;      (for [{:keys [id product manufacturer]} @models]
;        [:tr {:key id}
;         [:td id]
;         [:td product]
;         [:td manufacturer]])]]))


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
     [:table
      [:tr [:th "ID"] [:th "Product"] [:th "Manufacturer"] [:th "Edit"] [:th "Delete"]]
      (for [{:keys [id product manufacturer]} @models]
        [:tr {:key id}
         [:td id]
         [:td product]
         [:td manufacturer]
         [:td [:button {:on-click #(start-editing {:id id :product product :manufacturer manufacturer})} "Edit"]]
         [:td [:button {:on-click #(delete-model id)} "Delete"]]
         ])]]))

(defn page []
  [:<>
   [:h3 "Models"]
   [list-view]])
