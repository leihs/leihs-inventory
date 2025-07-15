(ns leihs.inventory.client.loader
  (:require
   ["react-router-dom" :as router]
   ["~/i18n.config.js" :as i18n :refer [i18n]]
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.utils :refer [jc]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; This file contains the loaders for the inventory routes.
;; The loaders are used to fetch data before rendering the components.
;; The root layout loader fetches the user profile and sets the language for i18n.
;; The other loaders fetch data for the specific pages, such as models, software, etc.
;;
;; Generic view data should be named with the `data` key in the returned map.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn root-layout []
  (-> http-client
      (.get "/inventory/profile")
      (.then (fn [res]
               (let [data (jc (.. res -data))]
                 (.. i18n (changeLanguage (-> data :user_details :language_locale)))
                 data)))
      (.catch (fn [error] (js/console.log "error" error) #js {}))))

(defn models-page [route-data]
  (let [params (.. ^js route-data -params)
        url (js/URL. (.. route-data -request -url))
        categories (-> http-client
                       (.get "/inventory/tree")
                       (.then #(jc (.-data %))))

        responsible-pools (-> http-client
                              (.get (router/generatePath
                                     "/inventory/:pool-id/responsible-inventory-pools"
                                     params))
                              (.then #(jc (.-data %))))

        models (-> http-client
                   (.get (str (.-pathname url) (.-search url)) #js {:cache false})
                   (.then #(jc (.. % -data))))]

    (.. (js/Promise.all (cond-> [categories models responsible-pools]))
        (then (fn [[categories models responsible-pools]]
                {:categories categories
                 :responsible-pools responsible-pools
                 :models models})))))

(defn software-crud-page [route-data]
  (let [params (.. ^js route-data -params)
        manufacturers (-> http-client
                          (.get "/inventory/manufacturers?type=Software" #js {:id "manufacturers"})
                          (.then #(remove (fn [el] (= "" el)) (jc (.-data %)))))

        software-id (or (:software-id (jc params)) nil)

        software-path (when software-id
                        (router/generatePath "/inventory/:pool-id/software/:software-id" params))

        data (when software-path
               (-> http-client
                   (.get software-path #js {:id software-id})
                   (.then #(jc (.-data %)))))]

    (.. (js/Promise.all (cond-> [manufacturers]
                          data (conj data)))
        (then (fn [[manufacturers & [data]]]
                {:manufacturers manufacturers
                 :data (if data data nil)})))))

(defn models-crud-page [route-data]
  (let [params (.. ^js route-data -params)
        path (router/generatePath "/inventory/:pool-id/entitlement-groups" params)
        entitlement-groups (-> http-client
                               (.get path)
                               (.then #(jc (.-data %))))

        categories (-> http-client
                       (.get "/inventory/tree")
                       (.then #(jc (.-data %))))

        models (-> http-client
                   (.get "/inventory/models-compatibles" #js {:id "compatible-models"})
                   (.then (fn [res]
                            (if (:model-id (jc params))
                              ;; If a model-id is provided, filter out the current model
                              (filter #(not= (:model_id %) (:model-id (jc params)))
                                      (jc (.-data res)))
                              ;; Otherwise, return all models
                              (jc (.-data res))))))

        manufacturers (-> http-client
                          (.get "/inventory/manufacturers?type=Model" #js {:id "manufacturers"})
                          (.then #(remove (fn [el] (= "" el)) (jc (.-data %)))))

        model-id (or (:model-id (jc params)) nil)

        model-path (when model-id
                     (router/generatePath "/inventory/:pool-id/model/:model-id" params))

        data (when model-path
               (-> http-client
                   (.get model-path #js {:id model-id})
                   (.then #(jc (.-data %)))))]

    (.. (js/Promise.all (cond-> [categories models manufacturers entitlement-groups]
                          data (conj data)))
        (then (fn [[categories models manufacturers entitlement-groups & [data]]]
                {:categories categories
                 :manufacturers manufacturers
                 :entitlement-groups entitlement-groups
                 :models models
                 :data (if data data nil)})))))

(defn items-crud-page [route-data]
  (let [models (-> http-client
                   (.get "/inventory/models-compatibles")
                   (.then #(jc (.-data %))))]

    (.. (js/Promise.all (cond-> [models]))
        (then (fn [[models]]
                {:models models})))))
