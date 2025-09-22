(ns leihs.inventory.server.utils.exception-handler
  (:require
   [ring.util.response :as resp :refer [bad-request response content-type]]
   [taoensso.timbre :refer [error]]))

(defn create-response-by-accept [accept status data]
  ;(println ">o> abc??" accept status data)
  (if (= accept "application/json")
    (-> (response data)
        (resp/status status))
    (-> (response "")
      (content-type "text/html")
      (resp/status status)))  )

(defn exception-handler [request message e]
                (error message (.getMessage e))
                (let [accept (get-in request [:headers "accept"])]
                  (cond
                    (instance? org.postgresql.util.PSQLException e)
                    (create-response-by-accept accept 409 {:status "failure"
                                                           :message message
                                                           :details (.getMessage e)})

                    (instance? clojure.lang.ExceptionInfo e)
                    (let [{:keys [status]} (ex-data e)
                          msg (ex-message e)]
                      (create-response-by-accept accept status {:status "failure"
                                                                :message message
                                                                :details msg}))

                    :else
                    (create-response-by-accept accept 400 {:status "failure"
                                                           :message message
                                                           :details (.getMessage e)}))))
