(ns leihs.inventory.server.utils.exception-handler
  (:require
   [clojure.string :as str]
   [cheshire.core :as json]
   [ring.util.response :as resp :refer [response content-type]]))

(defn create-response-by-accept [accept status data]
  (if (= accept "text/html")
    (-> (response "")
        (content-type "text/html")
        (resp/status status))
    (-> (response data)
        (resp/status status))))

(defn exception-handler [request message e]
  (let [accept (get-in request [:headers "accept"])]
    (cond
      (instance? org.postgresql.util.PSQLException e)
      (create-response-by-accept accept 409 {:status "failure"
                                             :message message
                                             :type (.getClass e)
                                             :details (.getMessage e)})

      (and (instance? clojure.lang.ExceptionInfo e) (str/includes? (.getMessage e) "Response coercion failed"))
      (->
        ;(response (json/generate-string {:type (.getMessage e)}) )
        (response {:type (.getMessage e)} )

        ;(content-type "application/json")
        (resp/status 500))

      (and (instance? clojure.lang.ExceptionInfo e) (str/includes? (.getMessage e) "Request coercion failed"))
      (->
        ;(response (json/generate-string {:type (.getMessage e)}) )
        (response {:type (.getMessage e)} )
        ;(content-type "application/json")
        (resp/status 422))


      (instance? clojure.lang.ExceptionInfo e)
      (let [{:keys [status]} (ex-data e)
            msg (ex-message e)]
        (create-response-by-accept accept status {:status "failure"
                                                  :message message
                                                  :type (.getClass e)
                                                  :details msg}))

      :else
      (create-response-by-accept accept 400 {:status "failure"
                                             :message message
                                             :type (.getClass e)
                                             :details (.getMessage e)}))))
