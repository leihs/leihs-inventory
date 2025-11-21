(ns leihs.inventory.server.utils.export
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [dk.ative.docjure.spreadsheet :as xl])
  (:import [java.io ByteArrayOutputStream]))

(defn csv-string
  "Converts a collection of maps to a CSV string.
   Takes an optional keys sequence to specify column order and selection.
   If no keys provided, uses keys from first map."
  ([data]
   (when (seq data)
     (csv-string data (keys (first data)))))
  ([data ks]
   (let [header (map name ks)
         rows (map (fn [m] (map #(get m %) ks)) data)
         all-rows (cons header rows)]
     (with-out-str
       (csv/write-csv *out* all-rows)))))

(defn csv-response
  "Creates a Ring response for CSV download from a collection of maps.
   Options:
   - :keys - sequence of keys to specify column order and selection
   - :filename - optional filename for Content-Disposition header"
  [data & {:keys [keys filename] :or {filename "export.csv"}}]
  (let [csv-string (if keys
                     (csv-string data keys)
                     (csv-string data))
        headers {"Content-Type" "text/csv; charset=utf-8"
                 "Content-Disposition" (str "attachment; filename=\"" filename "\"")}]
    {:status 200
     :headers headers
     :body csv-string}))

(defn excel-bytes
  "Converts a collection of maps to Excel XLSX bytes.
   Takes an optional keys sequence to specify column order and selection.
   If no keys provided, uses keys from first map.
   Options:
   - :keys - sequence of keys for column order
   - :sheet-name - name of the sheet (default: \"Sheet1\")"
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
  "Creates a Ring response for Excel download from a collection of maps.
   Options:
   - :keys - sequence of keys to specify column order and selection
   - :sheet-name - name of the sheet (default: \"Sheet1\")
   - :filename - optional filename for Content-Disposition header"
  [data & {:keys [keys sheet-name filename]
           :or {sheet-name "Sheet1" filename "export.xlsx"}}]
  (let [excel-bytes (excel-bytes data :keys keys :sheet-name sheet-name)
        headers {"Content-Type" "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                 "Content-Disposition" (str "attachment; filename=\"" filename "\"")}]
    {:status 200
     :headers headers
     :body (io/input-stream excel-bytes)}))

(comment
  ;; Test data
  (def test-data
    [{:id 1 :name "Alice" :age 30 :city "NYC"}
     {:id 2 :name "Bob" :age 25 :city "SF"}
     {:id 3 :name "Charlie" :age 35 :city "LA"}])

  ;; CSV export - all columns
  (csv-response test-data :filename "users.csv")

  ;; CSV export - selected columns in specific order
  (csv-response test-data
                :keys [:name :age]
                :filename "users-simple.csv")

  ;; Excel export - all columns
  (excel-response test-data :filename "users.xlsx")

  ;; Excel export - selected columns with custom sheet name
  (excel-response test-data
                  :keys [:name :city]
                  :sheet-name "Users"
                  :filename "users-by-city.xlsx")

  ;; Lower level: just get CSV string
  (csv-string test-data [:name :age])

  ;; Lower level: just get Excel bytes
  (excel-bytes test-data :keys [:id :name])

  ;; Empty data handling
  (csv-response [] :filename "empty.csv")
  (excel-response [] :filename "empty.xlsx"))
