(ns leihs.inventory.server.utils.session-dev-mode
  (:require
   [cheshire.core :as json]
   [leihs.inventory.server.utils.auth.session :refer [get-cookie-value]]
   [ring.util.codec :as codec]))

;; Format is an array of strings (needs to be url-encoded)
;; e.g.: leihs-dev-modes = ["dev-forms-v0","foo-bar"]

;; Example to set a cookie in browser console:
;; document.cookie = "leihs-dev-modes=" + encodeURIComponent(JSON.stringify(["dev-forms-v0", "foo-bar"])) + "; path=/inventory";

(def CONST_LEIHS_DEV_MODES :leihs-dev-modes)

(defn extract-dev-cookie-params [handler]
  (fn [request]
    (let [dev-modes (get-cookie-value request CONST_LEIHS_DEV_MODES)
          value (if (nil? dev-modes) [] (-> dev-modes
                                            codec/url-decode
                                            (json/parse-string vec)))
          request (assoc request :dev-modes value)]
      (handler request))))
