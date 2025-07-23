(ns leihs.inventory.client.loader
  (:require
   ["react-router-dom" :as router]
   ["~/i18n.config.js" :as i18n :refer [i18n]]

   [cljs.core.async :as async :refer [go]]
   [cljs.core.async.interop :refer-macros [<p!]]
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.utils :refer [jc cj]]))

(defn root-layout []
  (-> http-client
      (.get "/inventory/profile/")
      (.then (fn [res]
               (let [data (jc (.. res -data))]
                 (.. i18n (changeLanguage (-> data :user_details :language_locale)))
                 data)))
      (.catch (fn [error] (js/console.log "error" error) #js {}))))

(defn models-page [route-data]
  (let [url (js/URL. (.. route-data -request -url))
        search (.-search url)]
    (if (empty? search)
      (router/redirect "?page=1&size=50")
      (let [params (.. ^js route-data -params)
            pool-id (aget params "pool-id")
            categories (-> http-client
                           (.get (str "/inventory/" pool-id "/category-tree/"))
                           (.then #(jc (.-data %))))

            responsible-pools (-> http-client
                                  (.get (str "/inventory/" pool-id "/responsible-inventory-pools/"))
                                  (.then #(jc (.-data %))))

            models (-> http-client
                       (.get (str "/inventory/" pool-id "/list/" search) #js {:cache false})
                       (.then #(jc (.. % -data))))]

        (.. (js/Promise.all (cond-> [categories models responsible-pools]))
            (then (fn [[categories models responsible-pools]]
                    {:categories categories
                     :responsible-pools responsible-pools
                     :models models})))))))

(defn models-crud-page [route-data]
  (let [params (.. ^js route-data -params)
        pool-id (aget params "pool-id")
        model-id (or (aget params "model-id") nil)

        entitlement-groups (-> http-client
                               (.get (str "/inventory/" pool-id "/entitlement-groups/"))
                               (.then #(jc (.-data %))))

        categories (-> http-client
                       (.get (str "/inventory/" pool-id "/category-tree/"))
                       (.then #(jc (.-data %))))

        ;; models (-> http-client
        ;;            (.get (str "/inventory/" pool-id "/models/") #js {:id "compatible-models"})
        ;;            (.then (fn [res]
        ;;                     (if (:model-id (jc params))
        ;;                       ;; If a model-id is provided, filter out the current model
        ;;                       (filter #(not= (:model_id %) (:model-id (jc params)))
        ;;                               (jc (.. res -data -data)))
        ;;                       ;; Otherwise, return all models
        ;;                       (jc (.. res -data -data))))))

        manufacturers (-> http-client
                          (.get (str "/inventory/" pool-id "/manufacturers/?type=Model") #js {:id "manufacturers"})
                          (.then #(remove (fn [el] (= "" el)) (jc (.-data %)))))

        model-path (when model-id
                     (str "/inventory/" pool-id "/models/" model-id))

        data (when model-path
               (-> http-client
                   (.get model-path #js {:id model-id})
                   (.then #(jc (.-data %)))))]

    (.. (js/Promise.all (cond-> [categories manufacturers entitlement-groups]
                          data (conj data)))
        (then (fn [[categories manufacturers entitlement-groups & [data]]]
                {:categories categories
                 :manufacturers manufacturers
                 :entitlement-groups entitlement-groups
                 :data (if data data nil)})))))

(defn items-crud-page [route-data]
  (let [models (-> http-client
                   (.get "/inventory/models-compatibles")
                   (.then #(jc (.-data %))))]

    (.. (js/Promise.all (cond-> [models]))
        (then (fn [[models]]
                {:models models})))))
