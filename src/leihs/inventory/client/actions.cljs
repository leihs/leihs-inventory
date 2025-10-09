(ns leihs.inventory.client.actions
  (:require
   ["~/i18n.config.js" :as i18n :refer [i18n]]
   [cljs.core.async :as async :refer [go <!]]
   [cljs.core.async.interop :refer-macros [<p!]]
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]))

(defn root-layout [action]
  (go
    (let [request (<p! (.. action -request (json)))
          intent (aget request "intent")
          data (aget request "data")]
      (js/console.debug "root-layout action" intent data)

      (case intent
        "update-lang"
        (let [lang (.. data -lang)]
          (js/console.log "Language change requested to:" lang)
          (<p! (-> http-client
                   (.patch "/inventory/profile/" (cj {:language lang}))
                   (.then (fn [response]
                            #_(.. i18n (changeLanguage lang))
                            (js/console.debug "Language change response:" response)
                            nil))
                   (.catch (fn [error]
                             (js/console.error "Language change error:" error)
                             nil)))))))))
