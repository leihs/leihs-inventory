(ns leihs.inventory.client.lib.language
  (:require ["~/i18n.config.js" :as i18n :refer [i18n]]))

(defn switch-language [locale-id]
  (.. i18n (changeLanguage locale-id))
  (js/console.warn "Switching language, but should update the user profile on server too (to be implemented)")
  #_(let [url "/inventory/profile-language"
          data (cj {:locale locale-id,
                    csrf/token-field-name csrf/token})]
      (.. (js/fetch url (cj {:method "PUT"
                             :headers {"Accept" "application/json"}
                             :body (js/JSON.stringify data)}))
          (then (fn [data] (js/console.log "success" data)))
          (catch (fn [err] (js/console.log "error" err))))))