(ns leihs.inventory.client.actions
  (:require
   ["~/i18n.config.js" :as i18n :refer [i18n]]
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [promesa.core :as p]))

(defn profile [action]
  (p/let [request (.. action -request (formData))
          method (aget action "request" "method")]

    (case method
      "PATCH"
      (-> http-client
          (.patch "/inventory/profile/" request (cj {:cache
                                                     {:update {:profile "delete"}}}))
          (.then (fn [_]
                   (.. i18n (changeLanguage request))
                   #js {:status "ok"}))

          (.catch (fn [error]
                    (js/console.error "Language change error:" error)
                    #js {:error (.-message error)}))))))
