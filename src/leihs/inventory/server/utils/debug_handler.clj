(ns leihs.inventory.server.utils.debug-handler
  (:require
   [taoensso.timbre :refer [debug error]]))

(defn wrap-debug [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception ex
        (debug ex)
        (error (ex-message ex))
        (throw ex)))))
