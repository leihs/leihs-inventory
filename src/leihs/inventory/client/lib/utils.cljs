(ns leihs.inventory.client.lib.utils)

(defn jc [js]
  (js->clj js {:keywordize-keys true}))

(defn cj [clj]
  (clj->js clj))

(defn filter-map->filter-q-str
  "MQL filter map to EDN string for filter_q API param."
  [filter-map]
  (pr-str filter-map))

(defn filter-d-json->filter-q-str
  "Convert filter_d JSON (browser URL param) to EDN string for filter_q."
  [filter-d-json]
  (when (and filter-d-json (seq (str filter-d-json)))
    (-> filter-d-json
        js/JSON.parse
        jc
        filter-map->filter-q-str)))

(defn filter-d-json->filter-q-encoded
  "URL-encoded filter_q value from filter_d JSON."
  [filter-d-json]
  (some-> filter-d-json filter-d-json->filter-q-str js/encodeURIComponent))

(defn filter-d-json->filter-q-query-param
  "Query fragment `filter_q=<encoded>` from filter_d JSON."
  [filter-d-json]
  (when-let [encoded (filter-d-json->filter-q-encoded filter-d-json)]
    (str "filter_q=" encoded)))

(defn coerce-items-api-url
  "Rewrite items API URL: filter_d -> filter_q, drop page/size (export must not paginate)."
  [url]
  (let [base (or (.-origin js/window.location) "http://localhost")
        parsed (js/URL. url base)
        params (js/URLSearchParams. (.-search parsed))
        filter-d (.get params "filter_d")]
    (.delete params "filter_d")
    (.delete params "page")
    (.delete params "size")
    (when (and filter-d (seq filter-d))
      (when-let [filter-q (filter-d-json->filter-q-str filter-d)]
        (.set params "filter_q" filter-q)))
    (if (seq (.toString params))
      (str (.-pathname parsed) "?" (.toString params))
      (.-pathname parsed))))
