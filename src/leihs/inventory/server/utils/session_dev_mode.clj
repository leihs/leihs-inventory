(ns leihs.inventory.server.utils.session-dev-mode
  (:require [byte-streams :as bs]
            [cheshire.core :as json]
            [clojure.walk :refer [keywordize-keys]]
            [leihs.inventory.server.utils.auth.session :refer [get-cookie-value]]
            [ring.util.codec :as codec]))

;; Format is an array of strings (needs to be url-encoded)
;; e.g.: leihs-dev-modes = ["dev-forms-v0","foo-bar"]

;; Example to set a cookie in browser console:
;; document.cookie = "leihs-dev-modes=" + encodeURIComponent(JSON.stringify(["dev-forms-v0", "foo-bar"])) + "; path=/inventory";

(def CONST_LEIHS_DEV_MODES :leihs-dev-modes)
(def CONST_DEV_FORM_V0 "dev-forms-v0")

(defn get-dev-modes
  ([request]
   (:dev-modes request))
  ([request key]
   (some #(= key %) (:dev-modes request))))

(defn has-dev-mode-key-and-admin-permission
  ([request] "Used to protect by 'is_admin' and session-value='dev-forms-v0'"
             (has-dev-mode-key-and-admin-permission request CONST_DEV_FORM_V0))
  ([request key] "Used to protect by 'is_admin' and session-value"
                 (and (get-dev-modes request key)
                      (-> request :authenticated-entity :is_admin))))

(defn has-admin-permission "Used to protect by 'is_admin'"
  [request]
  (-> request :authenticated-entity :is_admin))

(defn extract-dev-cookie-params [handler]
  (fn [request]
    (let [cookie (-> request keywordize-keys :cookies)
          dev-modes (get-cookie-value request CONST_LEIHS_DEV_MODES)
          value (if (nil? dev-modes) [] (-> dev-modes
                                            codec/url-decode
                                            (json/parse-string vec)))
          request (assoc request :dev-modes value)]
      (handler request))))
