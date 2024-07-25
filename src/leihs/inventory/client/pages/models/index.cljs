(ns leihs.inventory.client.pages.models.index
  (:require [cljs.core.async :refer [<! go]]
            [clojure.string :as str]

   ;[leihs.inventory.client.pages.models.index :refer [error-msg]]

            [leihs.inventory.client.pages.models.api :as model-api]
            [reagent.core :as r]))

(def models (r/atom []))
(def error (r/atom nil))
(def editing (r/atom false))
(def editing (r/atom false))


(def current-model (r/atom nil))

(defn start-editing [model]
  (reset! editing true)
  (reset! current-model model))

(defn start-adding [model]
  (reset! editing false)
  (reset! current-model model))

(defn stop-editing []
  (reset! editing false))

(defn save-changes [id new-model]
  (edit-model id new-model)
  (stop-editing))


(defn delete-model [id]
  (go (let [response (<! (model-api/delete-model id))]
        (if (:success response)
          (do
            (println "Delete model" id)
            (reset! models (remove #(= (:id %) id) @models))
            (reset! error nil)
            )
          (reset! error (str "Error " (:status response)))

          ))))


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
      ;[:button {:on-click #(start-editing {:id "" :type "" :product "" :manufacturer ""})} "Add Model"]
      [:button {:on-click #(start-adding {:id "" :type "" :product "" :manufacturer ""})} "Add Model"]
      [:button {:on-click #(list-view)} "Refresh"]
      ]


     [:table
      [:tr [:th "ID"] [:th "Type"] [:th "Product"] [:th "Manufacturer"] [:th "Edit"] [:th "Delete"]]
      (for [{:keys [id type product manufacturer]} @models]
        [:tr {:key id}
         [:td id]
         [:td type]
         [:td product]
         [:td manufacturer]
         [:td [:button {:on-click #(start-editing {:id id :type type :product product :manufacturer manufacturer :process_update true})} "Edit"]]
         [:td [:button {:on-click #(delete-model id)} "Delete"]]
         ])]]))


(defn add-model [model]
  ;; Remove :id from the model
  (let [
        model-without-id (dissoc model :id )
        model-without-id (dissoc model-without-id :updated_at)
        model-without-id (dissoc model-without-id :created_at)

        ]
    (-> (js/fetch "/inventory/models"
          (clj->js {:method "POST"
                    :headers {"Accept" "application/json" "Content-Type" "application/json"}
                    :body (js/JSON.stringify (clj->js model-without-id))}))
      (.then (fn [response]
               (if (.-ok response)
                 (.json response)
                 (throw (js/Error. "Failed to save model")))))
      (.then (fn [data]
               (js/console.log "Model saved successfully:" data)))
      (.catch (fn [err]

                (println ">o> (defn add-model1.error=" error-msg)
                (println ">o> (defn add-model2.error=" err)
                (println ">o> (defn add-model3.error=" error)
                (println ">o>ERROR coming")
                ;(println ">o> (defn add-model1.error=" (.message error))
                ;(reset! error-msg (.message error))
                (reset! error err)
                ;(reset! @error (.message err))
                (println ">o> (defn add-model2")
                (js/console.error "Error saving model:" err)
                (println ">o> (defn add-model3")

                )))))

(defn update-model [model]
  ;; Remove :id from the model
  (let [

        model-without-id (dissoc model :id)
        model-without-id (dissoc model-without-id :process_update)

        ]
    (-> (js/fetch (str "/inventory/models/" (:id model))
          (clj->js {:method "PUT"
                    :headers {"Accept" "application/json" "Content-Type" "application/json"}
                    :body (js/JSON.stringify (clj->js model-without-id))}))
      (.then (fn [response]
               (if (.-ok response)
                 (.json response)
                 (throw (js/Error. "Failed to save model")))))
      (.then (fn [data]
               (js/console.log "Model saved successfully:" data)))
      (.catch (fn [err]

                (println ">o> (defn add-model1.error=" error-msg)
                (println ">o> (defn add-model2.error=" err)

                ;(println ">o> (defn add-model3.error=" (.message error))

                (reset! error (.message err))
                ;(reset! @error err)

                (println ">o> (defn add-model2")
                (js/console.error "Error saving model:" err)
                (println ">o> (defn add-model3")

                )))))


(defn nil-or-empty? [s]
  (str/blank? s))

(defn model-form []
  (let [new-model (r/atom {:id "" :product "" :type "" :manufacturer ""})
        ;(let [new-model (r/atom {:id "" :product "" :type "" :manufacturer ""})
        error-msg (r/atom nil)]
    (fn []
      [:div
       [:form {:on-submit (fn [e]
                            (.preventDefault e)

                            (println ">o> abc1. @new-model.new=" @new-model)
                            (println ">o> abc1. @new-model.cur=" @current-model)


                            (let [
                                  new-check (or
                                              ;(nil-or-empty? (:id @new-model))
                                              (nil-or-empty? (:product @new-model))
                                              (nil-or-empty? (:type @new-model))
                                              (nil-or-empty? (:manufacturer @new-model)))

                                  current-check (or
                                                  ;(nil-or-empty? (:id @new-model))
                                                  (nil-or-empty? (:product @current-model))
                                                  (nil-or-empty? (:type @current-model))
                                                  (nil-or-empty? (:manufacturer @current-model)))

                                  error-title (if @editing "Update-Check" "Add-Check")
                                  ]

                            (reset! error-msg nil)


                            (cond
                              (and @editing current-check)  (do
                                                                    (println ">o> abc2")
                                                                    (reset! error-msg (str error-title ": All fields are required.")))
                              (and (not @editing)  new-check)  (do
                                                                 (println ">o> abc2")
                                                                 (reset! error-msg (str error-title ": All fields are required.")))
                              :else                         (do
                                                              (println ">o> abc3")
                                                              (if @editing
                                                                (update-model @current-model)
                                                                (add-model @new-model))
                                                              (println ">o> abc4")
                                                              (reset! new-model {:id "" :product "" :type "" :manufacturer ""})
                                                              (reset! current-model {:product "" :type "" :manufacturer ""})
                                                              (reset! editing false)
                                                              (reset! error nil)
                                                              (list-view)
                                                              )


                              )))}

        [:label "Type"
         [:input {:type "text"
                  :value (if @editing (:type @current-model) (:type @new-model))
                  :on-change #(if @editing
                                (swap! current-model assoc :type (-> % .-target .-value))
                                (swap! new-model assoc :type (-> % .-target .-value)))}]]

        [:label "Product"
         [:input {:type "text"
                  :value (if @editing (:product @current-model) (:product @new-model))
                  :on-change #(if @editing
                                (swap! current-model assoc :product (-> % .-target .-value))
                                (swap! new-model assoc :product (-> % .-target .-value)))}]]
        [:label "Manufacturer"
         [:input {:type "text"
                  :value (if @editing (:manufacturer @current-model) (:manufacturer @new-model))
                  :on-change #(if @editing
                                (swap! current-model assoc :manufacturer (-> % .-target .-value))
                                (swap! new-model assoc :manufacturer (-> % .-target .-value)))}]]
        [:button (if @editing "Update" "Save")]]
       (when @error-msg
         [:div.error "Error: " @error-msg])])))

(defn page []
  [:<>
   [:h3 "Models"]
   [model-form]
   [:br]
   [list-view]])
