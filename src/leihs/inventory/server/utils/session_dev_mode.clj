(ns leihs.inventory.server.utils.session-dev-mode
  (:require [byte-streams :as bs]
            [cheshire.core :as json]
            [clojure.walk :refer [keywordize-keys]]
            [leihs.inventory.server.resources.auth.session :refer [get-cookie-value]]
            [ring.util.codec :as codec]))

(def CONST_LEIHS_DEV_MODES :leihs-dev-modes)
(def CONST_DEV_FORM_V0 "dev-forms-v0")

(defn get-dev-modes
  ([request]
   (:dev-modes request))
  ([request key]
   (some #(= key %) (:dev-modes request))))

(defn has-dev-mode-key-and-admin-permission
  [request key]
  (and (get-dev-modes request key)
       (-> request :authenticated-entity :is_admin)))

(defn has-admin-permission
  [request]
  (-> request :authenticated-entity :is_admin))

(defn extract-dev-cookie-params [handler]
  (fn [request]
    (try
      (let [cookie (-> request keywordize-keys :cookies)
            value (-> (get-cookie-value request CONST_LEIHS_DEV_MODES)
                      codec/url-decode
                      (json/parse-string vec))
            request (assoc request :dev-modes value)]
        (handler request))
      (catch Exception e
        (println "Error:" (.getMessage e))
        (handler request)))))