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


(defn delete-model [id]
  ;; Logic for deleting a model goes here
  (go (let [response (<! (model-api/delete-model id))]
        (if (:success response)
          (do
            (println "Delete model" id)
            (list-view))
          (reset! error (str "Error " (:status response)))))))


(defn delete-model [id]
  (go (let [response (<! (model-api/delete-model id))]
        (if (:success response)
          (do
            (println "Delete model" id)
            (reset! models (remove #(= (:id %) id) @models))
            (reset! error (str "Error " (:status response))))))))

  (defn add-model [model]
    ;; Logic for adding a model goes here
    (go (let [response (<! (model-api/add model))]
          (if (:success response)
            (do
              (println "Add model" model)
              (fetch-models))
            (reset! error (str "Error " (:status response)))))))


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

       [:div
        [:button {:on-click #(start-editing {:id "" :product "" :manufacturer ""})} "Add Model"]
        [:button {:on-click #(list-view)} "Refresh"]
        ]


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


(defn model-form []
  (let [new-model (r/atom {:id "" :product "" :manufacturer ""})]
    (fn []
      [:form {:on-submit (fn [e]
                           (.preventDefault e)
                           (add-model @new-model)
                           (reset! new-model {:id "" :product "" :manufacturer ""}))}
       [:label "ID"
        [:input {:type "text"
                 :value (:id @new-model)
                 :on-change #(swap! new-model assoc :id (-> % .-target .-value))}]]
       [:label "Product"
        [:input {:type "text"
                 :value (:product @new-model)
                 :on-change #(swap! new-model assoc :product (-> % .-target .-value))}]]
       [:label "Manufacturer"
        [:input {:type "text"
                 :value (:manufacturer @new-model)
                 :on-change #(swap! new-model assoc :manufacturer (-> % .-target .-value))}]]
       [:button "Save"]])))

  (defn page []
    [:<>
     [:h3 "Models"]
     [model-form]
     [:br]
     [list-view]])
