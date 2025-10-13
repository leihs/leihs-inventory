(ns leihs.inventory.client.actions
  (:require
   ["~/i18n.config.js" :as i18n :refer [i18n]]
   [cljs.core.async :as async :refer [go <!]]
   [cljs.core.async.interop :refer-macros [<p!]]
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [promesa.core :as p]))

(defn profile [action]
  (p/let [request (.. action -request (json))
          method (aget action "request" "method")]

    (case method
      "PATCH"
      (let []
        (-> http-client
            (.patch "/inventory/profile/" request)
            (.then (fn [response]
                     (.. i18n (changeLanguage (.-language request)))
                     #js {:status "ok"}))

            (.catch (fn [error]
                      (js/console.error "Language change error:" error)
                      #js {:error (.-message error)})))))))
