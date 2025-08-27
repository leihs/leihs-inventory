(ns leihs.inventory.server.resources.pool.models.model.images.image.thumbnail.main
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.request-utils :refer [path-params]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [debug error]])
  (:import
   [java.io ByteArrayInputStream]
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
      (debug e)
      {:status 400
       :body (str "Failed to decode Base64 string: " (.getMessage e))})))

(defn get-resource [request]
  (try
    (let [tx (:tx request)
          accept-header (get-in request [:headers "accept"])
          json-request? (= accept-header "application/json")
          image_id (-> request path-params :image_id)

          query (-> (sql/select :i.*)
                    (sql/from [:images :i])
                    (sql/where [:= :i.thumbnail true])
                    (cond-> image_id
                      (sql/where [:or [:= :i.id image_id] [:= :i.parent_id image_id]]))
                    (cond-> (not json-request?)
                      (sql/where [:= :i.content_type accept-header]))
                    sql-format)
          result (jdbc/execute-one! tx query)]

      (cond
        (nil? result) (status (response {:status "failure" :message "No thumbnail found"}) 404)
        (and json-request? image_id) (response result)
        (and json-request? (nil? image_id)) (response {:data result})
        (and (not json-request?) image_id) (convert-base64-to-byte-stream result)))
    (catch Exception e
      (debug e)
      (error "Failed to retrieve image:" (.getMessage e))
      (bad-request {:error "Failed to retrieve image" :details (.getMessage e)}))))
