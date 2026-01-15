(ns leihs.inventory.server.middlewares.request-params
  "Request preprocessing middleware - parses cookies and form params."
  (:require
   [byte-streams :as bs]
   [clojure.string :as str]
   [clojure.walk :refer [keywordize-keys]]
   [ring.util.codec :as codec]))

(defn parse-cookies [cookie-header]
  (->> (str/split cookie-header #"; ")
       (map #(str/split % #"="))
       (map (fn [[k v]] [(keyword k) {:value v}]))
       (into {})))

(defn add-cookies-to-request [request]
  (let [cookie-header (get-in request [:headers "cookie"])
        parsed-cookies (when cookie-header (parse-cookies cookie-header))]
    (assoc request :cookies parsed-cookies)))

(defn convert-params [request]
  (let [converted-form-params (into {} (map (fn [[k v]] [(keyword k) v]) (:form-params request)))]
    (-> request
        (assoc :form-params converted-form-params)
        (assoc :form-params-raw converted-form-params))))

(defn extract-form-params [stream]
  (try
    (let [body-str (bs/to-string stream)
          params (codec/form-decode body-str)
          keyword-params (keywordize-keys params)]
      keyword-params)
    (catch Exception _ nil)))

(defn wrap-parse-request [handler]
  (fn [request]
    (let [content-type (get-in request [:headers "content-type"])
          request (-> request
                      (cond-> (= content-type "application/x-www-form-urlencoded")
                        (assoc :form-params (some-> (:body request) extract-form-params)))
                      add-cookies-to-request
                      convert-params)]
      (handler request))))
