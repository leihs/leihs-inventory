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

(defn not-found
  "Loader that throws a 404 error to trigger the error boundary"
  []
  (let [error (js/Error "Page not found")]
    (set! (.-status error) 404)
    (set! (.-statusText error) "Not Found")
    (throw error)))

(defn request-error
  "Loader that throws a other than 404 error to trigger the error boundary"
  [status]
  (let [error (js/Error "Client error")]
    (set! (.-status error) status)
    (set! (.-statusText error) "Client error")
    (throw error)))

(defn server-error
  "Loader that throws a >=500 error to trigger the error boundary"
  [status]
  (let [error (js/Error "Server error")]
    (set! (.-status error) status)
    (set! (.-statusText error) "Server error")
    (throw error)))

(defn handle-error
  [err]
  (let [status (.-status err)]
    (cond
      (and (< status 500)
           (>= status 400))
      (request-error status)

      (>= status 500)
      (server-error status)

      :else
      (throw (js/Error "Loading route data failed"
                       #js {:cause "loader-error"})))))

(defn root-layout []
  (let [profile (-> http-client
                    (.get "/inventory/profile/" #js {:id "profile"})
                    (.then (fn [res] (jc (.. res -data)))))
        settings (-> http-client
                     (.get "/inventory/settings/")
                     (.then #(jc (.-data %))))]
    (.. (js/Promise.all (cond-> [profile settings]))
        (then (fn [[profile settings]]
                {:profile profile
                 :settings settings}))
        (catch (fn [err]
                 (handle-error err))))))

(defn list-page [route-data]
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
                                  (.get (str "/inventory/" pool-id "/inventory-pools/?responsible=true"))
                                  (.then #(jc (.-data %))))

            data (-> http-client
                     (.get (str "/inventory/" pool-id "/list/" search) #js {:cache false})
                     (.then #(jc (.. % -data))))]

        (.. (js/Promise.all (cond-> [categories data responsible-pools]))
            (then (fn [[categories data responsible-pools]]
                    {:categories categories
                     :responsible-pools responsible-pools
                     :data data}))
            (catch (fn [err]
                     (handle-error err))))))))

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
                 :data (if data data nil)}))
        (catch (fn [err]
                 (handle-error err))))))

(defn items-crud-page [route-data]
  (let [params (.. ^js route-data -params)
        pool-id (aget params "pool-id")
        item-id (or (aget params "item-id") nil)
        model-id (or (aget params "model-id") nil)

        item-path (when item-id
                    (str "/inventory/" pool-id "/items/"))

        model (when model-id
                (-> http-client
                    (.get (str "/inventory/" pool-id "/models/" model-id))
                    (.then #(jc (.-data %)))))

        data (if item-path
               (-> http-client
                   (.get (str "/inventory/" pool-id "/fields/?resource_id=" item-id "&target_type=item")
                         #js {:id item-id})
                   (.then #(jc (.-data %))))
               (-> http-client
                   (.get (str "/inventory/" pool-id "/fields/?target_type=item"))
                   (.then #(jc (.-data %)))))]

    (.. (js/Promise.all (cond-> [data] model (conj model)))
        (then (fn [[data & [model]]]
                {:data data
                 :model (if model model nil)}))
        (catch (fn [err]
                 (handle-error err))))))

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
                          (.get (str "/inventory/" pool-id "/manufacturers/?type=Model")
                                #js {:id "manufacturers"})
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
                 :data (if data data nil)}))
        (catch (fn [err]
                 (handle-error err))))))

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
        (then (fn [[& [data]]] {:data (if data data nil)}))
        (catch (fn [err]
                 (handle-error err))))))

(defn entitlement-groups-page [route-data]
  (let [url (js/URL. (.. route-data -request -url))
        search (.-search url)]
    (if (empty? search)
      (router/redirect "?page=1&size=50")
      (let [params (.. ^js route-data -params)
            search (.-search url)
            pool-id (aget params "pool-id")
            data (-> http-client
                     (.get (str "/inventory/" pool-id "/entitlement-groups/" search)
                           #js {:cache false})
                     (.then (fn [res]
                              (jc (.. res -data))))
                     (.catch (fn [error]
                               (js/console.error "Error fetching entitlement groups" error))))]

        (.. (js/Promise.all [data])
            (then (fn [[data]]
                    {:data data})))))))

(defn entitlement-group-crud-page [route-data]
  (let [params (.. ^js route-data -params)
        pool-id (aget params "pool-id")
        entitlement-group-id (or (aget params "entitlement-group-id") nil)

        entitlement-group-path (when entitlement-group-id
                                 (str "/inventory/" pool-id "/entitlement-groups/" entitlement-group-id))

        data (when entitlement-group-path
               (-> http-client
                   (.get entitlement-group-path #js {:id entitlement-group-id})
                   (.then #(jc (.-data %)))
                   (.catch (fn [error]
                             (js/console.error "Error fetching entitlement group" error)))))]

    (.. (js/Promise.all (cond-> [] data (conj data)))
        (then (fn [[& [data]]] {:data (if data data nil)})))))

(defn templates-page [route-data]
  (let [url (js/URL. (.. route-data -request -url))
        search (.-search url)]
    (if (empty? search)
      (router/redirect "?page=1&size=50")
      (let [params (.. ^js route-data -params)
            search (.-search url)
            pool-id (aget params "pool-id")
            data (-> http-client
                     (.get (str "/inventory/" pool-id "/templates/" search)
                           #js {:cache false})
                     (.then (fn [res]
                              (jc (.. res -data)))))]

        (.. (js/Promise.all [data])
            (then (fn [[data]]
                    {:data data}))
            (catch (fn [err]
                     (handle-error err))))))))

(defn template-crud-page [route-data]
  (let [params (.. ^js route-data -params)
        pool-id (aget params "pool-id")
        template-id (or (aget params "template-id") nil)

        template-path (when template-id
                        (str "/inventory/" pool-id "/templates/" template-id))

        data (when template-path
               (-> http-client
                   (.get template-path #js {:id template-id})
                   (.then #(jc (.-data %)))))]

    (.. (js/Promise.all (cond-> [] data (conj data)))
        (then (fn [[& [data]]] {:data (if data data nil)}))
        (catch (fn [_]
                 (throw (js/Error "Loading route data failed"
                                  #js {:cause "loader-error"})))))))

(defn error-test
  "Test loader that always throws an error"
  []
  (throw (js/Error "Test loader error - this should trigger the error boundary!"
                   #js {:cause "loader-error"})))
