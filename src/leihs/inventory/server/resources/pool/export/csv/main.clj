(ns leihs.inventory.server.resources.pool.export.csv.main
  (:require
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [clojure.set]
   [clojure.string :as str]
   [dk.ative.docjure.spreadsheet :as ss]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :as log]
   [taoensso.timbre :refer [error]]))

(defn keyword-to-title [k]
  (-> k
      name
      (str/replace "-" " ")
      (str/replace "_" " ")
      (str/capitalize)))

(defn maps-to-csv [maps]
  (let [headers (map keyword-to-title (keys (first maps)))
        rows (map vals maps)]
    (cons headers rows)))

(defn index-resources [request]
  "Handler that generates an CSV file from a given map."
  (let [data [{:name "Alice" :age 30}
              {:name "Bob" :age 25}]
        output-stream (java.io.ByteArrayOutputStream.)
        csv-data (maps-to-csv data)]
    (with-open [writer (io/writer output-stream)]
      (csv/write-csv writer csv-data))
    {:status 200
     :headers {"Content-Type" "text/csv"
               "Content-Disposition" "attachment; filename=output.csv"}
     :body (java.io.ByteArrayInputStream. (.toByteArray output-stream))}))
