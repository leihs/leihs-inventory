(ns leihs.inventory.server.resources.pool.models.model.attachments.attachment.main
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.request-utils :refer [path-params]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response]]
   [taoensso.timbre :refer [error]])
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

(defn convert-base64-to-byte-stream [result content-disposition]
  (try
    (let [content-type (:content_type result)
          base64-str (:content result)
          decoded-bytes (decode-base64-str base64-str)]
      {:status 200
       :headers {"Content-Type" content-type
                 "Content-Disposition" content-disposition}
       :body (io/input-stream (ByteArrayInputStream. decoded-bytes))})
    (catch IllegalArgumentException e
      {:status 400
       :body (str "Failed to decode Base64 string: " (.getMessage e))})))

(defn get-resource [request]
  (try
    (let [tx (:tx request)
          id (-> request path-params :attachments_id)
          model-id (-> request path-params :model_id)
          accept-header (get-in request [:headers "accept"])
          json-request? (= accept-header "application/json")
          octet-request? (= accept-header "application/octet-stream")

          content-disposition (or (-> request :parameters :query :content_disposition) "inline")
          type (or (-> request :parameters :query :type) "new")
          query (-> (sql/select :a.*)
                    (sql/from [:attachments :a])
                    (cond-> model-id (sql/where [:= :a.model_id model-id]))
                    (cond-> id (sql/where [:= :a.id id]))
                    (cond-> (and (not json-request?) (not octet-request?))
                      (sql/where [:= :a.content_type accept-header]))
                    sql-format)
          attachment (jdbc/execute-one! tx query)
          base64-string (:content attachment)
          file-name (:filename attachment)
          content-type (:content_type attachment)]

      (if (nil? attachment)
        (do
          (error "Attachment not found" {:id id :model-id model-id})
          (bad-request {:error "Attachment not found"}))
        (cond (= accept-header "application/octet-stream")
              (->> base64-string
                   (.decode (Base64/getMimeDecoder))
                   (hash-map :body)
                   (merge {:headers {"Content-Type" content-type
                                     "Content-Transfer-Encoding" "binary"
                                     "Content-Disposition" (str content-disposition "; filename=\"" file-name "\"")}}))

              (= accept-header "application/json")
              (response attachment)

              :else (convert-base64-to-byte-stream attachment content-disposition))))

    (catch Exception e
      (error "Failed to get attachments" e)
      (bad-request {:error "Failed to get attachments" :details (.getMessage e)}))))

(defn delete-resource [{:keys [tx] :as request}]
  (let [{:keys [attachments_id]} (path-params request)
        res (jdbc/execute-one! tx
                               (-> (sql/delete-from :attachments)
                                   (sql/where [:= :id attachments_id])
                                   sql-format))]
    (if (= (:next.jdbc/update-count res) 1)
      (response {:status "ok" :attachments_id attachments_id})
      (bad-request {:error "Failed to delete attachment"}))))
