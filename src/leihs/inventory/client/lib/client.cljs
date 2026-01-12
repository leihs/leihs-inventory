(ns leihs.inventory.client.lib.client
  (:require ["axios" :as axios]
            ["axios-cache-interceptor" :as axios-cache]
            ["react-router-dom" :as router]
            [leihs.inventory.client.lib.csrf :as csrf]
            [leihs.inventory.client.lib.utils :refer [cj]]))

(def instance
  (.. axios -default (create
                      #js {:baseURL ""
                           :headers #js {"Content-Type" "application/json"
                                         "Accept" "application/json"}
                           :xsrfCookieName csrf/cookie-name
                           :xsrfHeaderName csrf/header-field-name})))

(def http-client (axios-cache/setupCache instance))

(defn- split-path-and-query
  "Splits a path into [base-path query-string].
   Returns [path nil] if no query string exists."
  [path]
  (let [idx (.indexOf path "?")]
    (if (= idx -1)
      [path nil]
      [(.substring path 0 idx) (.substring path (inc idx))])))

(defn safe-concat
  "Concatenates a path and a value with URI-encoding of the value."
  [path value]
  (let [value (js/encodeURIComponent value)]
    (str path value)))

(defn safe-query
  "Constructs a relative path with URI-encoded query parameters.
  
  Parameters:
  - path: Base path string (may contain existing query params)
  - params: Clojure map of query parameters OR string (when concatenate is true)
  
  Parses existing query params from path, merges with params.
  Empty params preserves existing query params.
  
  Examples:
  (safe-query \"/api/models\" {:type \"model\"}) 
  => \"/api/models?type=model\"
  
  (safe-query \"/api/models/?type=model\" {:page 2}) 
  => \"/api/models/?type=model&page=2\"
  
  (safe-query \"/api/models/?type=model\" {})
  => \"/api/models/?type=model\""
  [path params]
  (let [[base-path query-string] (split-path-and-query path)
        existing-params-obj (router/createSearchParams (or query-string ""))
        existing-params (into {}
                              (map (fn [k] [(keyword k) (.get existing-params-obj k)])
                                   (js/Array.from (.keys existing-params-obj))))
        merged-params (merge existing-params (or params {}))]
    (if (empty? merged-params)
      base-path
      (let [query-params (router/createSearchParams (cj merged-params))]
        (str base-path "?" (.toString query-params))))))

