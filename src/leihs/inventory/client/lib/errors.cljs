(ns leihs.inventory.client.lib.errors
  (:require
   ["sonner" :refer [toast]]
   ["~/i18n.config.js" :refer [i18n]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :refer [$]]))

(defn error-result
  "Turns a rejected axios request into a result map."
  [err]
  (let [res (.-response err)]
    {:status (some-> res .-status)
     :data (some-> res .-data jc)}))

(defn action-result
  "Turns the error result of a route action into a result map."
  [data]
  {:status (aget data "httpStatus")
   :data (jc (aget data "response"))})

(defn- server-message [data]
  (when (map? data)
    (or (:message data)
        (:error data)
        (get-in data [:errors 0 :message]))))

(defn- detail-lines [t status data]
  (let [fields (when (map? data) (:fields data))]
    (cons (when (pos? status)
            (t "error.action.error_detail" #js {:httpStatus status}))
          (if (seq fields)
            (map (fn [[field code]]
                   (str (name field) " " (t (str "error.validation." code))))
                 fields)
            [(when (< status 500) (server-message data))]))))

(defn toast-error!
  "Shows an error toast for a failed request result. The title is the first
   existing i18n key of `title-keys`, else a generic one by status."
  [t {:keys [status data]} title-keys]
  (let [status (or status 0)
        title-key (or (some #(when (.exists i18n %) %)
                            (concat title-keys [(str "error.status." status)]))
                      "error.status.default")
        lines (remove empty? (detail-lines t status data))]
    (.. toast (error (t title-key)
                     (cj {:description (when (seq lines)
                                         ($ :span
                                            (for [[i line] (map-indexed vector lines)]
                                              ($ :p {:key i} line))))})))))
