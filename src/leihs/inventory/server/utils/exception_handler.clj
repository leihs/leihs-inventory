(ns leihs.inventory.server.utils.exception-handler
  (:require
   [cheshire.core :as json]
   [clojure.string :as str]
   [cheshire.core :as json]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.walk]
   [leihs.inventory.server.utils.helper :refer [log-by-severity]]
   [ring.util.response :as resp :refer [content-type response]]
  [taoensso.timbre :refer [warn]])
(:import
 [java.io ByteArrayInputStream]))


(def CONST_COERCION_REQUEST_ERROR_HTTP_CODE 422)
(def CONST_COERCION_RESPONSE_ERROR_HTTP_CODE 500)

(defn- extract-data-from-input-stream [input-stream]
  (when (instance? java.io.ByteArrayInputStream input-stream)
    (slurp input-stream)))

(defn- pretty-print-json [json-str]
  (json/generate-string (json/parse-string json-str true) {:pretty true}))

(defn- contains-substrings? [s substrings]
  (and s (every? #(re-find (re-pattern (java.util.regex.Pattern/quote %)) s) substrings)))

(defn- has-coercion-substring? [s]
  (println ">o> abc.s" s)
  (if (nil? s) false
               (boolean (re-find #"\"coercion\"\s*:\s*\"(spec|schema)\"" s))))

(defn- parse-edn-strings [m]
  (clojure.walk/postwalk
    (fn [x]
      (if (and (string? x)
            (re-find #"^\(" x)) ;; crude check for EDN-ish string
        (try
          (edn/read-string x)
          (catch Exception e (log-by-severity "Error parsing edn-string" e) x))
        x))
    m))

(defn- beautify-problems [problems]
  (map (fn [problem]
         (-> problem
           (dissoc :path :via)
           (update :in #(str/join "/" %))))
    problems))

(defn- data->input-stream [data]
  (-> (if (instance? String data)
        data
        (json/generate-string data))
    (.getBytes "UTF-8")
    (ByteArrayInputStream.)))

(defn- is-coercion-error? [data]
  (or (contains-substrings? data ["schema" "errors" "type" "coercion" "value" "in"])
    (contains-substrings? data ["problems"])))

(defn- extract-coercion-reason
  ([data req]
   (extract-coercion-reason data req true))

  ([data req with-errors?]
   (let [parsed-data (-> data (json/parse-string true) parse-edn-strings)
         coercion (:coercion parsed-data)
         is-coercion? (some #{"spec" "schema"} [coercion])]
     (when is-coercion?
       (let [errors (or (get-in parsed-data [:messages])
                      (beautify-problems (:problems parsed-data)))
             scope (some->> (:in parsed-data) (map str) (str/join "/"))
             status (if (str/includes? scope "response")
                      CONST_COERCION_RESPONSE_ERROR_HTTP_CODE
                      CONST_COERCION_REQUEST_ERROR_HTTP_CODE)
             base-resp {:reason "Coercion-Error"
                        :scope scope
                        :coercion-type coercion
                        :uri (str (str/upper-case (name (:request-method req)))
                               " " (:uri req))}
             full-resp (if with-errors?
                         (assoc base-resp :messages errors)
                         base-resp)]
         {:is-coercion-error true
          :response-status status
          :response-data full-resp})))))

(defn- generate-coercion-response [data req resp]
  (println ">o> abc.generate-coercion-response" )
  (warn (pretty-print-json data))
  (let [{:keys [response-status response-data]}
        (extract-coercion-reason data req false)]
    (assoc resp
      :body (data->input-stream response-data)
      :status response-status)))

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
        (response {:type (.getMessage e)})

        ;(content-type "application/json")
        (resp/status 500))

      (and (instance? clojure.lang.ExceptionInfo e) (str/includes? (.getMessage e) "Request coercion failed"))

      (let [
            data (.getData e)
            message (.getMessage e)
            ctype (str (get-in data [:coercion]))
            ctype (cond
                    (clojure.string/includes? ctype "schema") :schema
                    (clojure.string/includes? ctype "spec") :spec
                    :else :unknown)

            ;               _ (println ">o> name:" (:name (into {} ctype)))
            ;
            ;p (println ">o> abc.ctype0" ctype (type ctype))
            ;
            ;               ctype (:name (:coercion data))
            ;p (println ">o> abc.ctype1a" ctype)
            ;
            ;               ctype (get-in data [:coercion :name])
            ;p (println ">o> abc.ctype1" ctype)
            ;
            ;               ctype (get-in data [:coercion "name"])
            ;p (println ">o> abc.ctype2" ctype (type ctype))
            ;
            ;               ctype (name (:name (:coercion (.getData e))))
            ;p (println ">o> abc.ctype3" ctype)

            ;scope (:in data)

            scope (str/join "/" (map name (:in data)))

            uri (:uri request)
            method (clojure.string/upper-case (name (:request-method request)))

            parsed-data (-> data (json/parse-string true) parse-edn-strings)
            _ (warn (pretty-print-json parsed-data))

            ]
        (->
          ;(response (json/generate-string {:type (.getMessage e)}) )
          (response {:reason "Coercion-Error"
                     :detail message
                     :coercion-type ctype
                     :scope scope
                     :uri (str method " " uri)
                     })
          ;(content-type "application/json")
          (resp/status 422))
        )


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
