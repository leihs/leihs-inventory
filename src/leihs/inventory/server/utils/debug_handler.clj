(ns leihs.inventory.server.utils.debug-handler
  (:require [logbug.thrown :as thrown]
            [taoensso.timbre :refer [debug error]]))

(defonce ^:private DEBUG true)

(defn debug-mode? [] DEBUG)

(defn wrap-debug [handler]
  (fn [request]
    (let [wrap-debug-level (or (:wrap-debug-level request) 0)]
      (try
        (debug "RING-LOGGING-WRAPPER"
               {:wrap-debug-level wrap-debug-level
                :request request})
        (let [response (handler (assoc request :wrap-debug-level (inc wrap-debug-level)))]
          (debug "RING-LOGGING-WRAPPER"
                 {:wrap-debug-level wrap-debug-level
                  :response response})
          response)
        (catch Exception ex
          (error "RING-LOGGING-WRAPPER CAUGHT EXCEPTION "
                 {:wrap-debug-level wrap-debug-level} (ex-message ex))
          (error "RING-LOGGING-WRAPPER CAUGHT EXCEPTION " ex)
          (throw ex))))))
