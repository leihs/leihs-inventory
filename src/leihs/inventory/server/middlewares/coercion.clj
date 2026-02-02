(ns leihs.inventory.server.middlewares.coercion
  (:require
   [cheshire.core :as json]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.walk]
   [leihs.inventory.server.middlewares.debug :refer [log-by-severity]]
   [leihs.inventory.server.utils.response :as rh]
   [taoensso.timbre :refer [debug]])
  (:import
   [java.io ByteArrayInputStream]))

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
             status (cond
                      (str/includes? scope "response")
                      CONST_COERCION_RESPONSE_ERROR_HTTP_CODE

                      (or (str/includes? scope "path-params")
                          (str/includes? scope "query-params"))
                      404

                      :else 422)
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
  (debug (pretty-print-json data))
  (let [{:keys [response-status response-data]}
        (extract-coercion-reason data req false)]
    (assoc resp
           :body (data->input-stream response-data)
           :status response-status)))

(defn handle-coercion-error [request resp]
  (let [status (:status resp)
        accept-header (str/lower-case (or (get-in request [:headers "accept"]) ""))
        is-image? (str/includes? accept-header "image/")
        uri (:uri request)
        check-result (and (= status 500) is-image? (str/includes? uri "/inventory"))]
    (debug "handle-coercion-error called" "status:" status "accept:" accept-header "is-image?:" is-image? "uri:" uri "check:" check-result)
    (cond
      ;; Pass through auth/authz errors unchanged
      (or (= status 401) (= status 403))
      (do (debug "Passing through auth error") resp)

      ;; 500 error on image/* request → assume coercion error from auth failure, return 401
      check-result
      (do
        (debug "Converting 500 coercion error to 401 for image/* request")
        {:status 401
         :headers {"content-type" "application/json"}
         :body (json/generate-string {:status "failure" :message "Not authenticated"})})

      ;; Otherwise continue with existing logic
      :else
      (let [is-html? (str/includes? accept-header "text/html")
            uri (:uri request)
            is-inventory? (str/includes? uri "/inventory")
            is-attachment? (str/includes? uri "/attachments/")
            is-error-status? (and status (>= status 400))]
        (cond
         ;; HTML request to /inventory attachment with error → keep text/plain response
          (and is-html? is-inventory? is-attachment? is-error-status?)
          resp

         ;; HTML request to /inventory (non-attachment) with error status → return SPA
          (and is-html? is-inventory? (not is-attachment?) is-error-status?)
          (rh/index-html-response request status)

         ;; HTML request to /inventory (non-attachment) with string body containing error → return SPA
          (and is-html? is-inventory? (not is-attachment?) (string? (:body resp))
               (or (str/includes? (:body resp) "Coercion-Error")
                   (str/includes? (:body resp) "coercion")))
          (rh/index-html-response request (or status 500))

         ;; HTML request to /inventory (non-attachment) with ByteArrayInputStream → check for error
          (and is-html? is-inventory? (not is-attachment?) (instance? java.io.ByteArrayInputStream (:body resp)))
          (let [ext-data (extract-data-from-input-stream (:body resp))]
            (if (and ext-data
                     (or (str/includes? ext-data "Coercion-Error")
                         (str/includes? ext-data "coercion")))
              (rh/index-html-response request (or status 500))
             ;; Not an error - restore body and continue
              (assoc resp :body (data->input-stream ext-data))))

         ;; String body (non-HTML or non-inventory) - return as-is
          (string? (:body resp))
          resp

         ;; Check if body contains coercion error (for JSON requests)
          (and (= accept-header "application/json")
               (instance? java.io.ByteArrayInputStream (:body resp)))
          (let [ext-data (extract-data-from-input-stream (:body resp))]
            (if (and ext-data
                     (has-coercion-substring? ext-data)
                     (is-coercion-error? ext-data))
              (generate-coercion-response ext-data request resp)
              (assoc resp :body (data->input-stream ext-data))))

         ;; Non-JSON requests - return as-is
          (not= accept-header "application/json")
          resp

          :else resp)))))

(defn wrap-handle-coercion-error [handler]
  (fn [request]
    (let [response (handler request)]
      (handle-coercion-error request response))))
