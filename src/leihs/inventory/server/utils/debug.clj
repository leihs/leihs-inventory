; This namespace needs to be enabled in logging.cljc in shared-clj
; in order to activate app-wide debug stacktraces.

(ns leihs.inventory.server.utils.debug
  (:require
   [taoensso.timbre :refer [debug error]]))

(defn log-by-severity
  ([e]
   (log-by-severity nil e))
  ([message e]
   (if (nil? message)
     (error (.getMessage e))
     (error message (.getMessage e)))
   (debug e)))

(defn wrap-debug [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception ex
        (debug ex)
        (error (ex-message ex))
        (throw ex)))))
