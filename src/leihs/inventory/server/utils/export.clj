(ns leihs.inventory.server.utils.export
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [dk.ative.docjure.spreadsheet :as xl]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as jdbc-rs]
            [taoensso.timbre :as timbre :refer [debug spy]])
  (:import [java.io ByteArrayOutputStream]))

(defn csv-string
  [[header & rows]]
  (let [all-rows (cons (map name header) rows)]
    (with-out-str
      (csv/write-csv *out* all-rows))))

(defn csv-response
  [data & {:keys [filename] :or {filename "export.csv"}}]
  (let [csv-string (csv-string data)
        headers {"Content-Type" "text/csv; charset=utf-8"
                 "Content-Disposition" (str "attachment; filename=\"" filename "\"")}]
    {:status 200
     :headers headers
     :body csv-string}))

(defn excel-bytes
  [data & {:keys [keys sheet-name] :or {sheet-name "Sheet1"}}]
  (let [ks (or keys (when (seq data) (clojure.core/keys (first data))))
        header (map name ks)
        rows (map (fn [m] (map #(get m %) ks)) data)
        all-rows (cons header rows)
        wb (xl/create-workbook sheet-name all-rows)
        out (ByteArrayOutputStream.)]
    (xl/save-workbook! out wb)
    (.toByteArray out)))

(defn excel-response
  [data & {:keys [keys sheet-name filename]
           :or {sheet-name "Sheet1" filename "export.xlsx"}}]
  (let [excel-bytes (excel-bytes data :keys keys :sheet-name sheet-name)
        headers {"Content-Type" "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                 "Content-Disposition" (str "attachment; filename=\"" filename "\"")}]
    {:status 200
     :headers headers
     :body (io/input-stream excel-bytes)}))

(defn jdbc-execute! [tx sql]
  (jdbc/execute! tx sql {:builder-fn jdbc-rs/as-unqualified-arrays}))

(defn arrays-to-maps
  "Convert JDBC array result (header + rows) to maps"
  [[header & rows]]
  (let [keys (map keyword header)]
    (map (fn [row]
           (zipmap keys row))
         rows)))
