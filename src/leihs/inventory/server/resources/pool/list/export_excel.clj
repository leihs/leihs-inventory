(ns leihs.inventory.server.resources.pool.list.export-excel
  (:require
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [clojure.set]
   [clojure.string :as str]
   [dk.ative.docjure.spreadsheet :as ss]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.helper :refer [now-yyyyMMdd-HHmmss]]
   [leihs.inventory.server.resources.pool.items.filter-handler :as filter]
   [leihs.inventory.server.resources.pool.items.main :as helper]
   [leihs.inventory.server.utils.debug :refer [log-by-severity]]
   [taoensso.timbre :refer [debug]])
  (:import [java.time Instant]) )

(defn generate-excel-from-map
  "Generates an Excel file from a map and returns a Java File object."
  [data-map]
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
      (log-by-severity "Failed to generate Excel from map" e)
      (throw e))))

(defn convert
  "Handler that generates an Excel file from a given map."
  [data]
  (try
    (let [excel-file (generate-excel-from-map (or data []))]
      {:status 200
       :headers {"Content-Type" "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                 "x-rows" (str (count data))
                 "Content-Disposition"
                 (format "attachment; filename=\"export-%s.xlsx\"; x-generated-at=\"%s\""
                   (now-yyyyMMdd-HHmmss)
                   (str (Instant/now)))}
       :body (io/input-stream excel-file)})
    (catch IllegalArgumentException e
      (log-by-severity "Invalid input to Excel handler" e)
      {:status 400
       :body "Invalid input to generate Excel file."})
    (catch Exception e
      (log-by-severity "Internal Server Error in Excel handler" e)
      {:status 500
       :body "Internal Server Error."})))
