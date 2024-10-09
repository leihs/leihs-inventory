(ns leihs.inventory.server.resources.attachments.main
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

(defn get-attachments-handler [request]
  (try
    (let [tx (:tx request)
          id (-> request path-params :id)
          query (-> (sql/select :a.*)
                    (sql/from [:attachments :a])
                    (cond-> id (sql/where [:= :a.id id]))
                    (sql/limit 10)
                    sql-format)
          result (jdbc/query tx query)]
      (response result))
    (catch Exception e
      (error "Failed to get attachments" e)
      (bad-request {:error "Failed to get attachments" :details (.getMessage e)}))))
