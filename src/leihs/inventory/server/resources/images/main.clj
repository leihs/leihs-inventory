(ns leihs.inventory.server.resources.images.main
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

(defn handle-base64-image-request [result]
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
          image_id (-> request path-params :id)
          is-thumbnail? (str/ends-with? (:uri request) "/thumbnail")
          query (-> (sql/select :i.*)
                    (sql/from [:images :i])
                    (sql/where [:= :i.thumbnail is-thumbnail?])
                    (cond-> image_id
                      (sql/where [:= :i.target_id
                                  (-> (sql/select :i.target_id)
                                      (sql/from [:images :i])
                                      (sql/where [:= :i.id image_id]))]))
                    (sql/limit 2)
                    sql-format)
          result (jdbc/query tx query)]

      (cond
        (and json-request? image_id) (response result)
        (and json-request? (nil? image_id)) (response {:data result})
        (and (not json-request?) image_id) (handle-base64-image-request (first result))))
    (catch Exception e
      (error "Failed to retrieve image" e)
      (bad-request {:error "Failed to retrieve image" :details (.getMessage e)}))))
