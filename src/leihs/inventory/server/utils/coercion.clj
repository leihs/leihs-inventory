(ns leihs.inventory.server.utils.coercion
  (:require
   [cheshire.core :as json]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.walk]
   [leihs.inventory.server.utils.debug :refer [log-by-severity]]
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
  (warn (pretty-print-json data))
  (let [{:keys [response-status response-data]}
        (extract-coercion-reason data req false)]
    (assoc resp
           :body (data->input-stream response-data)
           :status response-status)))

(defn- accept-html? [accept-header]
  (when accept-header
    (or (str/includes? accept-header "text/html")
        (str/includes? accept-header "*/*"))))

(defn- generate-plain-text-coercion-response [data req resp]
  (warn (pretty-print-json data))
  (let [parsed-data (-> data (json/parse-string true) parse-edn-strings)
        scope (some->> (:in parsed-data) (map str) (str/join "/"))
        status (if (str/includes? (str scope) "response")
                 CONST_COERCION_RESPONSE_ERROR_HTTP_CODE
                 CONST_COERCION_REQUEST_ERROR_HTTP_CODE)]
    (-> resp
        (assoc :body "coercion error")
        (assoc :status status)
        (assoc :headers (assoc (:headers resp {}) "Content-Type" "text/plain")))))

(defn handle-coercion-error [request resp]
  (let [accept-header (get-in request [:headers "accept"])
        is-html? (accept-html? accept-header)
        is-json? (= accept-header "application/json")
        body (:body resp)]
    (cond
      (and (not is-html?) (not is-json?)) resp

      (and (string? body) is-html?
           (has-coercion-substring? body)
           (is-coercion-error? body))
      (generate-plain-text-coercion-response body request resp)

      (string? body) resp

      (instance? java.io.ByteArrayInputStream body)
      (let [ext-data (extract-data-from-input-stream body)]
        (if (and ext-data
                 (has-coercion-substring? ext-data)
                 (is-coercion-error? ext-data))
          (if is-html?
            (generate-plain-text-coercion-response ext-data request resp)
            (generate-coercion-response ext-data request resp))
          (assoc resp :body (data->input-stream ext-data))))

      :else resp)))

(defn wrap-handle-coercion-error [handler]
  (fn [request]
    (let [response (handler request)]
      (handle-coercion-error request response))))
