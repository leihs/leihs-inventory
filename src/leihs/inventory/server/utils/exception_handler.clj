(ns leihs.inventory.server.utils.exception-handler
  (:require
   [ring.util.response :as resp :refer [bad-request response status]]
   [taoensso.timbre :refer [error]]))

(defn exception-handler [message e]
  (error message (.getMessage e))
  (cond
    (instance? org.postgresql.util.PSQLException e)
    (-> (response {:status "failure"
                   :message message
                   :detail (.getMessage e)})
        (status 409))

    (instance? clojure.lang.ExceptionInfo e)
    (let [{:keys [status]} (ex-data e)
          msg (ex-message e)]
      (-> (response {:status "failure"
                     :error msg})
          (resp/status status)))

    :else (bad-request {:error message :details (.getMessage e)})))
