(ns leihs.inventory.server.utils.exception-handler
  (:require
   [cheshire.core :as json]
   [clojure.string :as str]
   [clojure.walk]
   [leihs.inventory.server.utils.helper :refer [log-by-severity]]
   [ring.util.response :as resp :refer [content-type response]]
   [taoensso.timbre :refer [warn]])
  (:import
   [clojure.lang ExceptionInfo]
   [org.postgresql.util PSQLException]))

(defn- pretty-print-json [data]
  (json/generate-string data {:pretty true}))

(defn create-response-by-accept [accept status data]
  (if (= accept "text/html")
    (-> (response "")
        (content-type "text/html")
        (resp/status status))
    (-> (response data)
        (resp/status status))))

(defn- build-coercion-response [request e response-status]
  (let [data (.getData e)
        message (.getMessage e)
        ctype (str (:coercion data))
        ctype (cond
                (str/includes? ctype "schema") :schema
                (str/includes? ctype "spec") :spec
                :else :unknown)
        scope (some->> (:in data) (map name) (str/join "/"))
        uri (:uri request)
        method (-> request :request-method name str/upper-case)
        resp-map {:reason "Coercion-Error"
                  :detail message
                  :coercion-type ctype
                  :scope scope
                  :uri (str method " " uri)}]
    (warn (pretty-print-json resp-map))
    (-> (response resp-map)
        (resp/status response-status))))

(defn exception-handler [request message e]
  (let [accept (get-in request [:headers "accept"])]
    (cond
      (instance? PSQLException e)
      (create-response-by-accept accept 409 {:status "failure"
                                             :message message
                                             :type (class e)
                                             :details (.getMessage e)})

      (and (instance? ExceptionInfo e)
           (str/includes? (.getMessage e) "Response coercion failed"))
      (build-coercion-response request e 500)

      (and (instance? ExceptionInfo e)
           (str/includes? (.getMessage e) "Request coercion failed"))
      (build-coercion-response request e 422)

      (instance? ExceptionInfo e)
      (let [{:keys [status]} (ex-data e)
            msg (ex-message e)]
        (create-response-by-accept accept status {:status "failure"
                                                  :message message
                                                  :type (class e)
                                                  :details msg}))

      :else (create-response-by-accept accept 400 {:status "failure"
                                                   :message message
                                                   :type (class e)
                                                   :details (.getMessage e)}))))

(defn wrap-exception
  "Middleware that catches exceptions and delegates to exception-handler."
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log-by-severity e)
        (exception-handler request "wrap-exception" e)))))
