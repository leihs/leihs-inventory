(ns leihs.inventory.server.middlewares.exception-handler
  (:require
   [cheshire.core :as json]
   [clojure.string :as str]
   [clojure.walk]
   [leihs.inventory.server.middlewares.debug :refer [log-by-severity]]
   [ring.util.response :as resp :refer [content-type response]]
   [taoensso.timbre :refer [warn]])
  (:import
   [clojure.lang ExceptionInfo]
   [org.postgresql.util PSQLException]))

(defn- pretty-print-json [data]
  (json/generate-string data {:pretty true}))

(defn create-response-by-accept [request accept status data]
  (let [uri (:uri request)
        is-attachment? (str/includes? uri "/attachments/")
        error-msg (or (:details data) (:message data) "Error")]
    (cond
      (and (= accept "text/html") is-attachment?)
      (-> (response error-msg)
          (content-type "text/plain")
          (resp/status status))

      (= accept "text/html")
      (-> (response "")
          (content-type "text/html")
          (resp/status status))

      :else
      (-> (response (json/generate-string data))
          (content-type "application/json")
          (resp/status status)))))

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
                  :uri (str method " " uri)}
        accept (get-in request [:headers "accept"])
        is-attachment? (str/includes? uri "/attachments/")]
    (warn (pretty-print-json resp-map))
    (if (and (= accept "text/html") is-attachment?)
      (-> (response message)
          (content-type "text/plain")
          (resp/status response-status))
      (-> (response (json/generate-string resp-map))
          (content-type "application/json")
          (resp/status response-status)))))

(defn exception-handler [request message e]
  (let [accept (get-in request [:headers "accept"])
        uri (:uri request)
        authenticated? (-> request :authenticated-entity boolean)
        is-image? (and accept (str/includes? (str/lower-case accept) "image/"))
        is-inventory? (and uri (str/includes? uri "/inventory"))]
    (cond
      (instance? PSQLException e)
      (create-response-by-accept request accept 409 {:status "failure"
                                                     :message message
                                                     :type (str (class e))
                                                     :details (.getMessage e)})

      ;; Response coercion on image/* to /inventory when not authenticated → 401
      (and (instance? ExceptionInfo e)
           (str/includes? (.getMessage e) "Response coercion failed")
           is-image?
           is-inventory?
           (not authenticated?))
      (-> (response (json/generate-string {:status "failure" :message "Not authenticated"}))
          (content-type "application/json")
          (resp/status 401))

      (and (instance? ExceptionInfo e)
           (str/includes? (.getMessage e) "Response coercion failed"))
      (build-coercion-response request e 500)

      (and (instance? ExceptionInfo e)
           (str/includes? (.getMessage e) "Request coercion failed"))
      (let [data (.getData e)
            scope-in (:in data)
            status (if (some #{:path-params :query-params} scope-in) 404 422)]
        (build-coercion-response request e status))

      (instance? ExceptionInfo e)
      (let [{:keys [status]} (ex-data e)
            msg (ex-message e)]
        (create-response-by-accept request accept status {:status "failure"
                                                          :message message
                                                          :type (str (class e))
                                                          :details msg}))

      :else (create-response-by-accept request accept 400 {:status "failure"
                                                           :message message
                                                           :type (str (class e))
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
