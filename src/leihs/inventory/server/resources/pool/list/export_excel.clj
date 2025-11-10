(ns leihs.inventory.server.resources.pool.list.export-excel
  (:require
   [clojure.set]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [leihs.core.core :refer [presence]]
   [leihs.inventory.server.resources.pool.models.common :refer [fetch-thumbnails-for-ids
                                                                model->enrich-with-image-attr]]
   [leihs.inventory.server.resources.pool.models.queries :refer [base-inventory-query
                                                                 filter-by-type
                                                                 from-category
                                                                 with-items
                                                                 with-search
                                                                 without-items]]




[leihs.inventory.server.resources.pool.export.csv.main :refer [maps-to-csv]]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [clojure.java.io :as io]
   [clojure.set]
   [dk.ative.docjure.spreadsheet :as ss]

   [leihs.inventory.server.utils.debug :refer [log-by-severity]]


   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [clojure.set]
   [clojure.string :as str]
   [ring.middleware.accept]

   [leihs.inventory.server.resources.pool.items.main :as helper]
   [leihs.inventory.server.resources.pool.items.filter-handler :as filter]
   [leihs.inventory.server.resources.pool.fields.main :refer [fetch-properties-fields]]

   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [leihs.inventory.server.utils.request-utils :refer [path-params
                                                       query-params]]
   [ring.util.response :refer [response]]
   [taoensso.timbre :refer [debug]]))

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
    (let [ excel-file (generate-excel-from-map data)]
      {:status 200
       :headers {"Content-Type" "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                 "x-rows" (str (count data))
                 "Content-Disposition" "attachment; filename=export.xlsx"}
       :body (io/input-stream excel-file)})
    (catch IllegalArgumentException e
      (log-by-severity "Invalid input to Excel handler" e)
      {:status 400
       :body "Invalid input to generate Excel file."})
    (catch Exception e
      (log-by-severity "Internal Server Error in Excel handler" e)
      {:status 500
       :body "Internal Server Error."})))
