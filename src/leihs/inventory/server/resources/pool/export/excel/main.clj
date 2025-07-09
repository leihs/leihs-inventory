(ns leihs.inventory.server.resources.pool.export.excel.main
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

(defn generate-excel-from-map [data-map]
  "Generates an Excel file from a map and returns a Java File object."
  (try
    (when (empty? data-map)
      (throw (IllegalArgumentException. "Data map cannot be empty")))
    (let [workbook (ss/create-workbook "Sheet1" [(map name (keys (first data-map)))])
          sheet (ss/select-sheet "Sheet1" workbook)]
      (doseq [row data-map]
        (let [row-values (map #(if (or (string? %) (number? %)) % (str %)) (vals row))]
          (ss/add-row! sheet row-values)))
      (let [temp-file (doto (java.io.File/createTempFile "export" ".xlsx") (.deleteOnExit))]
        (with-open [output-stream (io/output-stream temp-file)]
          (ss/save-workbook! output-stream workbook))
        temp-file))
    (catch Exception e
      (log/error e "Failed to generate Excel from map")
      (throw e))))

(defn index-resources [request]
  "Handler that generates an Excel file from a given map."
  (try
    (let [data [{:name "Alice" :age 30 :city "New York"}
                {:name "Bob" :age 25 :city "San Francisco"}
                {:name "Charlie" :age 35 :city "Boston"}]
          excel-file (generate-excel-from-map data)]
      {:status 200
       :headers {"Content-Type" "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                 "Content-Disposition" "attachment; filename=export.xlsx"}
       :body (io/input-stream excel-file)})
    (catch IllegalArgumentException e
      (log/error e "Invalid input to Excel handler")
      {:status 400
       :body "Invalid input to generate Excel file."})
    (catch Exception e
      (log/error e "Internal Server Error in Excel handler")
      {:status 500
       :body "Internal Server Error."})))
