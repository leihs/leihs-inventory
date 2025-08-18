(ns leihs.inventory.server.utils.exception-handler
  (:require
   [ring.util.response :as resp :refer [bad-request response status]]
   [taoensso.timbre :refer [error]]))

(defn exception-to-response
  "Handles exceptions thrown during request processing."
  [_ e default-error]
  (error default-error (.getMessage e))
  (let [response (cond
                   (instance? org.postgresql.util.PSQLException e)
                   (cond
                     (str/includes? (.getMessage e) "unique_model_name_idx")
                     (-> (response {:status "failure" :message "Model already exists"})
                         (status 409))
                     (str/includes? (.getMessage e) "insert or update on table \"models_compatibles\"")
                     (-> (response {:status "failure" :message "Modification of models_compatibles failed"})
                         (status 409))
                     :else nil)
                   (instance? clojure.lang.ExceptionInfo e)
                   (bad-request {:status "failure" :error default-error
                                 :details {:message (.getMessage e)
                                           :scope (when-let [data (safe-ex-data e)] (:scope data))}})
                   :else nil)]
    (or response
        (bad-request {:status "failure" :error default-error
                      :details {:message (.getMessage e)}}))))
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
