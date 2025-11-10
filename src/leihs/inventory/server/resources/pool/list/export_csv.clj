(ns leihs.inventory.server.resources.pool.list.export-csv
  (:require
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [clojure.set]
   [clojure.string :as str]
   [dk.ative.docjure.spreadsheet :as ss]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]

   [leihs.inventory.server.utils.helper :refer [now-yyyyMMdd-HHmmss]]

   ;[java.time Instant]
   ;[java.time.format DateTimeFormatter]
   ;[java.time.ZoneOffset :as ZoneOffset]
   ;[java.time.LocalDateTime :as LocalDateTime]

   [leihs.inventory.server.resources.pool.export.csv.main :refer [maps-to-csv]]
   [leihs.inventory.server.resources.pool.items.filter-handler :as filter]
   [leihs.inventory.server.resources.pool.items.main :as helper]
   [taoensso.timbre :refer [debug]])
  (:import (java.time Instant)))


;(ns leihs.inventory.server.utils.helper
;  (:require
;   [clojure.string :as str]
;   [clojure.walk :as walk]
;   [java.time Instant LocalDateTime ZoneOffset]
;   [java.time.format DateTimeFormatter]
;   [taoensso.timbre :refer [error debug]]))


;(defn now-yyyyMMdd-HHmmss

(defn convert "Handler that generates an CSV file from a given map."
  [data]
  (let [output-stream (java.io.ByteArrayOutputStream.)
        csv-data (maps-to-csv data)  ]
    (with-open [writer (io/writer output-stream)]
      (csv/write-csv writer csv-data))
    {:status 200
     :headers {"Content-Type" "text/csv"
               "x-rows" (str (count data))
               "Content-Disposition"
               (format "attachment; filename=\"output-%s.csv\"; x-generated-at=\"%s\""
                 now-yyyyMMdd-HHmmss
                 (str (Instant/now)))}

     :body (java.io.ByteArrayInputStream. (.toByteArray output-stream))}))
