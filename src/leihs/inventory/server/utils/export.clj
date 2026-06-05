(ns leihs.inventory.server.utils.export
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [leihs.inventory.server.utils.export-result-set-stream :as rs-stream])
  (:import [java.io File FileInputStream FileOutputStream
            FilterInputStream OutputStream Writer]
           [java.lang.ref Cleaner]
           [org.apache.poi.ss.usermodel Cell Row Sheet]
           [org.apache.poi.xssf.streaming SXSSFWorkbook]))

(defn- cell-value->string
  [value]
  (if (nil? value) "" (str value)))

(defn- set-cell-value!
  "Keep booleans/numeric primitives typed; stringify complex values (e.g. Long/date) to avoid OOM regressions."
  [^Cell cell value]
  (cond
    (nil? value)
    (.setBlank cell)

    (boolean? value)
    (.setCellValue cell ^boolean value)

    (instance? Double value)
    (.setCellValue cell ^double value)

    (instance? Float value)
    (.setCellValue cell (double value))

    (instance? java.math.BigDecimal value)
    (.setCellValue cell (.doubleValue ^java.math.BigDecimal value))

    :else
    (.setCellValue cell (cell-value->string value))))

(defn- column-label->string
  [label]
  (if (keyword? label) (name label) (str label)))

(defn- write-excel-header!
  [^Sheet sheet column-labels]
  (let [^Row header-row (.createRow sheet 0)]
    (doseq [[idx label] (map-indexed vector column-labels)]
      (set-cell-value! (.createCell header-row (int idx))
                       (column-label->string label)))))

(defn- write-excel-data-row!
  [^Sheet sheet row-index row]
  (let [^Row sheet-row (.createRow sheet row-index)]
    (doseq [[idx value] (map-indexed vector row)]
      (set-cell-value! (.createCell sheet-row (int idx)) value))))

(defn- export-content-headers
  [filename content-type]
  {"Content-Type" content-type
   "Content-Disposition" (str "attachment; filename=\"" filename "\"")})

(def ^:private tempfile-cleaner (Cleaner/create))

(defn- delete-on-close-input-stream
  [^java.io.File file]
  (let [in (FileInputStream. file)
        deleted (atom false)
        delete-once! (fn []
                       (when (compare-and-set! deleted false true)
                         (try
                           (.delete file)
                           (catch Exception _))))
        cleanable-ref (atom nil)]
    (let [wrapper (proxy [FilterInputStream] [in]
                    (close []
                      (try
                        (proxy-super close)
                        (finally
                          (delete-once!)
                          (when-let [c @cleanable-ref]
                            (.clean c))))))]
      (reset! cleanable-ref
              (.register tempfile-cleaner wrapper
                         (reify Runnable
                           (run [_]
                             (delete-once!)))))
      wrapper)))

(defn- write-csv-to-stream!
  [^OutputStream out tx sql fetch-size]
  (with-open [^Writer writer (io/writer out :encoding "UTF-8")]
    (rs-stream/with-result-set-stream
      tx sql
      {:fetch-size fetch-size
       :on-metadata (fn [column-labels]
                      (csv/write-csv writer [(mapv column-label->string column-labels)]))
       :on-row (fn [row]
                 (csv/write-csv writer [(mapv cell-value->string row)]))})))

(defn- write-excel-to-stream!
  [^OutputStream out tx sql {:keys [sheet-name fetch-size]
                             :or {sheet-name "Sheet1"}}]
  (let [wb (doto (SXSSFWorkbook. 100) (.setCompressTempFiles true))
        sheet (.createSheet wb sheet-name)
        row-index (atom 0)]
    (try
      (rs-stream/with-result-set-stream
        tx sql
        {:fetch-size fetch-size
         :on-metadata (fn [column-labels]
                        (write-excel-header! sheet column-labels)
                        (reset! row-index 1))
         :on-row (fn [row]
                   (write-excel-data-row! sheet @row-index row)
                   (swap! row-index inc))})
      (.write wb out)
      (finally
        (.dispose wb)
        (.close wb)))))

(defn csv-stream-response
  [tx sql & {:keys [filename fetch-size]
             :or {filename "export.csv"}}]
  (let [tmp-file (File/createTempFile "leihs-inventory-export-" ".csv")]
    (try
      (with-open [out (FileOutputStream. tmp-file)]
        (write-csv-to-stream! out tx sql fetch-size))
      {:status 200
       :headers (export-content-headers filename "text/csv; charset=utf-8")
       :body (delete-on-close-input-stream tmp-file)}
      (catch Exception e
        (.delete tmp-file)
        (throw e)))))

(defn excel-stream-response
  [tx sql & {:keys [filename sheet-name fetch-size]
             :or {filename "export.xlsx"
                  sheet-name "Sheet1"}}]
  (let [tmp-file (File/createTempFile "leihs-inventory-export-" ".xlsx")]
    (try
      (with-open [out (FileOutputStream. tmp-file)]
        (write-excel-to-stream! out tx sql
                                {:sheet-name sheet-name
                                 :fetch-size fetch-size}))
      {:status 200
       :headers (export-content-headers
                 filename
                 "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
       :body (delete-on-close-input-stream tmp-file)}
      (catch Exception e
        (.delete tmp-file)
        (throw e)))))


