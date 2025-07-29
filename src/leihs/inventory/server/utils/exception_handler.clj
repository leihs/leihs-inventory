(ns leihs.inventory.server.utils.exception-handler
  (:require
   [clojure.string :as str]
   [leihs.inventory.server.utils.helper :refer [safe-ex-data]]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]]))

(defn exception-to-response
  "Handles exceptions thrown during request processing."
  [request e default-error]
  (error default-error (.getMessage e))
  (let [status-code (if (instance? clojure.lang.ExceptionInfo e)
                      (:status (ex-data e))
                      500)
        response (cond
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
