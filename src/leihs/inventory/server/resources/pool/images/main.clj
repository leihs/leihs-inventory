(ns leihs.inventory.server.resources.pool.images.main
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [next.jdbc.sql :as jdbc]
   [ring.util.response :refer [bad-request response]]
   [taoensso.timbre :refer [error]])
  (:import [java.io ByteArrayInputStream]
           [java.util Base64]))

(defn- clean-base64-string [base64-str]
  (clojure.string/replace base64-str #"\s+" ""))

(defn- url-safe-to-standard-base64 [base64-str]
  (-> base64-str
      (clojure.string/replace "-" "+")
      (clojure.string/replace "_" "/")))

(defn- add-padding [base64-str]
  (let [mod (mod (count base64-str) 4)]
    (cond
      (= mod 2) (str base64-str "==")
      (= mod 3) (str base64-str "=")
      :else base64-str)))

(defn- decode-base64-str [base64-str]
  (let [cleaned-str (-> base64-str
                        clean-base64-string
                        url-safe-to-standard-base64
                        add-padding)
        decoder (Base64/getDecoder)]
    (.decode decoder cleaned-str)))

(defn convert-base64-to-byte-stream [result]
  (try
    (let [content-type (:content_type result)
          base64-str (:content result)
          decoded-bytes (decode-base64-str base64-str)]
      {:status 200
       :headers {"Content-Type" content-type
                 "Content-Disposition" "inline"}
       :body (io/input-stream (ByteArrayInputStream. decoded-bytes))})
    (catch IllegalArgumentException e
      {:status 400
       :body (str "Failed to decode Base64 string: " (.getMessage e))})))

(defn get-image-thumbnail-handler [request]
  (try
    (let [tx (:tx request)
          accept-header (get-in request [:headers "accept"])
          json-request? (= accept-header "application/json")

          query (-> (sql/select :i.*)
                    (sql/from [:images :i])

                    sql-format)
          result (jdbc/query tx query)]

      (cond
        json-request?  (response {:data result})
       :else   (convert-base64-to-byte-stream (first result))))
    (catch Exception e
      (error "Failed to retrieve image:" (.getMessage e))
      (bad-request {:error "Failed to retrieve image" :details (.getMessage e)}))))
