(ns leihs.inventory.client.loader
  (:require
   ["react-router-dom" :as router]
   ["~/i18n.config.js" :as i18n :refer [i18n]]
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.utils :refer [jc]]))

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

        model (when model-path
                (-> http-client
                    (.get model-path #js {:id model-id})
                    (.then (fn [res]
                             (let [kv (jc (.-data res))]
                               (->> kv
                                    (vals)
                                    (map (fn [el] (if (nil? el) "" el)))
                                    (zipmap (keys kv))))))))]

    (.. (js/Promise.all (cond-> [categories models manufacturers entitlement-groups]
                          model (conj model)))
        (then (fn [[categories models manufacturers entitlement-groups & [model]]]
                {:categories categories
                 :manufacturers manufacturers
                 :entitlement-groups entitlement-groups
                 :models models
                 :model (if model model nil)})))))

(defn items-crud-page [route-data]
  (let [models (-> http-client
                   (.get "/inventory/models-compatibles")
                   (.then #(jc (.-data %))))]

    (.. (js/Promise.all (cond-> [models]))
        (then (fn [[models]]
                {:models models})))))
