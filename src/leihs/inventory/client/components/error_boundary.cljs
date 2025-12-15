(ns leihs.inventory.client.components.error-boundary
  (:require [uix.core :as uix :refer [defui $]]))

(def error-boundary
  (uix/create-error-boundary
   {:derive-error-state (fn [error] {:error error}) ;; maps JS Error into component's state
    :did-catch (fn [error info] (println error))} ;; for side effects e.g. logging an error into your tracing system etc
   (fn [[{:keys [error]} set-state] {:keys [children]}] ;; signature ([state set-state] props)
     (if error
       ($ :div {:style {:color :red}}
          error)
       children))))

(defui erroring-component []
  (throw "Hey! Open browser's console to inspect the error"))
