(ns leihs.inventory.client.lib.csrf
  (:require [leihs.core.constants :as constants]))

(defn get-cookie [name]
  (let [re (js/RegExp. (str "(?:^|; )" name "=([^;]*)"))
        match (.match (.-cookie js/document) re)]
    (when match
      (js/decodeURIComponent (aget match 1)))))

(def token
  (get-cookie constants/ANTI_CSRF_TOKEN_COOKIE_NAME))

(def token-field-name constants/ANTI_CSRF_TOKEN_FORM_PARAM_NAME)