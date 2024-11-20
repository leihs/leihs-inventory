(ns leihs.inventory.server.resources.attachments.main
  (:require
   [clojure.data.codec.base64 :as base64]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [next.jdbc.sql :as jdbc]
   [ring.util.response :as response]
   [ring.util.response :refer [bad-request response]]
   [taoensso.timbre :refer [error]])
  (:import [java.io ByteArrayInputStream]))

(defn base64-to-content-stream
  [base64-string file-name]
  (let [decoded-bytes (base64/decode (.getBytes base64-string "UTF-8"))
        input-stream (ByteArrayInputStream. decoded-bytes)]
    (-> (response/response input-stream)
        (response/content-type "application/octet-stream")
        (response/header "Content-Disposition" (str "attachment; filename=\"" file-name "\"")))))

(defn get-attachments-handler [request]
  (try
    (let [tx (:tx request)
          id (-> request path-params :id)
          accept-header (get-in request [:headers "accept"])
          uri (:uri request)
          query (-> (sql/select :a.*)
                    (sql/from [:attachments :a])
                    (cond-> id (sql/where [:= :a.id id]))
                    sql-format)
          result (jdbc/query tx query)]
      (cond
        (= accept-header "application/octet-stream")
        (let [attachment (first result)
              base64-string (:content attachment)
              file-name (:filename attachment)]
          (base64-to-content-stream base64-string file-name))
        :else
        (response {:attachments result})))
    (catch Exception e
      (error "Failed to get attachments" e)
      (bad-request {:error "Failed to get attachments" :details (.getMessage e)}))))
