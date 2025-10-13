(ns leihs.inventory.client.loader
  (:require
   ["react-router-dom" :as router]
   ["~/i18n.config.js" :as i18n :refer [i18n]]
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.utils :refer [jc cj]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; This file contains the loaders for the inventory routes.
;; The loaders are used to fetch data before rendering the components.
;; The root layout loader fetches the user profile and sets the language for i18n.
;; The other loaders fetch data for the specific pages, such as models, software, etc.
;;
;; Generic view data should be named with the `data` key in the returned map.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn profile []
  (-> http-client
      (.get "/inventory/profile/")
      (.then (fn [res]
               (let [data (jc (.. res -data))]
                 data)))
      (.catch (fn [error] (js/console.log "error" error) #js {}))))

(defn root-layout []
  (-> http-client
      (.get "/inventory/profile/" #js {:id "profile"})
      (.then (fn [res]
               (let [data (jc (.. res -data))]
                 #_(.. i18n (changeLanguage (-> data :user_details :language_locale)))
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

            data (-> http-client
                     (.get (str "/inventory/" pool-id "/list/" search) #js {:cache false})
                     (.then #(jc (.. % -data))))]

        (.. (js/Promise.all (cond-> [categories data responsible-pools]))
            (then (fn [[categories data responsible-pools]]
                    {:categories categories
                     :responsible-pools responsible-pools
                     :data data})))))))

(defn software-crud-page [route-data]
  (let [params (.. ^js route-data -params)
        pool-id (aget params "pool-id")
        manufacturers (-> http-client
                          (.get (str "/inventory/" pool-id "/manufacturers/?type=Software") #js {:id "manufacturers"})
                          (.then #(remove (fn [el] (= "" el)) (jc (.-data %)))))

        software-id (or (:software-id (jc params)) nil)

        software-path (when software-id (str "/inventory/" pool-id "/software/" software-id))

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
        pool-id (aget params "pool-id")
        model-id (or (aget params "model-id") nil)

        entitlement-groups (-> http-client
                               (.get (str "/inventory/" pool-id "/entitlement-groups/"))
                               (.then #(jc (.-data %))))

        categories (-> http-client
                       (.get (str "/inventory/" pool-id "/category-tree/"))
                       (.then #(jc (.-data %))))

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

(defn options-crud-page [route-data]
  (let [params (.. ^js route-data -params)
        pool-id (aget params "pool-id")
        option-id (or (aget params "option-id") nil)

        option-path (when option-id
                      (str "/inventory/" pool-id "/options/" option-id))

        data (when option-path
               (-> http-client
                   (.get option-path #js {:id option-id})
                   (.then #(jc (.-data %)))))]

    (.. (js/Promise.all (cond-> [] data (conj data)))
        (then (fn [[& [data]]] {:data (if data data nil)})))))

(defn templates-page [route-data]
  (let [url (js/URL. (.. route-data -request -url))
        search (.-search url)]
    (if (empty? search)
      (do
        (js/console.debug "Redirecting to ?page=1&size=50")
        (router/redirect "?page=1&size=50"))
      (let [params (.. ^js route-data -params)
            search (.-search url)
            pool-id (aget params "pool-id")
            data (-> http-client
                     (.get (str "/inventory/" pool-id "/templates/" search)
                           #js {:cache false})
                     (.then (fn [res]
                              (jc (.. res -data))))
                     (.catch (fn [error]
                               (js/console.error "Error fetching templates" error))))]

        (.. (js/Promise.all [data])
            (then (fn [[data]]
                    {:data data})))))))

(defn template-crud-page [route-data]
  (let [params (.. ^js route-data -params)
        pool-id (aget params "pool-id")
        template-id (or (aget params "template-id") nil)

        template-path (when template-id
                        (str "/inventory/" pool-id "/templates/" template-id))

        data (when template-path
               (-> http-client
                   (.get template-path #js {:id template-id})
                   (.then #(jc (.-data %)))
                   (.catch (fn [error]
                             (js/console.error "Error fetching template" error)))))]

    (.. (js/Promise.all (cond-> [] data (conj data)))
        (then (fn [[& [data]]] {:data (if data data nil)})))))

(defn items-crud-page [route-data]
  (let [models (-> http-client
                   (.get "/inventory/models-compatibles")
                   (.then #(jc (.-data %))))]

    (.. (js/Promise.all (cond-> [models]))
        (then (fn [[models]]
                {:models models})))))
