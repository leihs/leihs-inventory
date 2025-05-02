(ns leihs.inventory.server.utils.debug-handler
  (:require
   [taoensso.timbre :refer [debug error]]))

(defn wrap-debug [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception ex
        (error (ex-message ex))
        (debug ex)
        (throw ex)))))
