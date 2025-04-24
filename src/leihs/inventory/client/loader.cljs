(ns leihs.inventory.client.loader
  (:require
   ["react-router-dom" :as router]
   ["~/i18n.config.js" :as i18n :refer [i18n]]
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.utils :refer [jc]]))

(defn root-layout []
  (-> http-client
      (.get "/inventory/profile")
      (.then #(jc (.. % -data)))
      (.then #(do (.. i18n (changeLanguage (-> % :user_details :language_locale)))
                  %))))

(defn models-page [route-data]
  (let [url (js/URL. (.. route-data -request -url))]
    (-> http-client
        (.get (str (.-pathname url) (.-search url)))
        (.then #(jc (.. % -data))))))

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
                   (.get "/inventory/models-compatibles")
                   (.then #(jc (.-data %))))

        manufacturers (-> http-client
                          (.get "/inventory/manufacturers?type=Model")
                          (.then #(remove (fn [el] (= "" el)) (jc (.-data %)))))

        model-path (when (:model-id (jc params)) (router/generatePath "/inventory/:pool-id/model/:model-id" params))

        model (when model-path
                (-> http-client
                    (.get model-path)
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
