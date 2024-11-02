(ns leihs.inventory.server.resources.export.routes
  (:require
   [clojure.set]

   [ring.util.response :as response]
   [clojure.string :as str]
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [dk.ative.docjure.spreadsheet :as ss]

   [leihs.inventory.server.resources.auth.session :as session]
   [leihs.inventory.server.resources.models.main :refer [get-models-handler
                                                         create-model-handler
                                                         update-model-handler
                                                         delete-model-handler]]
   [leihs.inventory.server.resources.models.models-by-pool :refer [get-models-of-pool-handler
                                                                   create-model-handler-by-pool
                                                                   get-models-of-pool-handler
                                                                   update-model-handler-by-pool
                                                                   delete-model-handler-by-pool]]
   [leihs.inventory.server.resources.export.main :refer [get-suppliers-handler
                                                         get-suppliers-auto-pagination-handler]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]
   [taoensso.timbre :as log])) ;; Added timbre for logging







(defn write-csv [filename maps]
  (let [header (keys (first maps))
        rows (map vals maps)]
    (with-open [writer (io/writer filename)]
      (csv/write-csv writer (cons header rows)))))

;(defn write-xls [filename maps]
;  (let [header (keys (first maps))
;        rows (map vals maps)
;        workbook (spreadsheet/create-workbook "Sheet1" (cons header rows))]
;    (spreadsheet/save-workbook! filename workbook)))

;; Example usage
(def data [{:name "Alice" :age 30} {:name "Bob" :age 25}])

(write-csv "output.csv" data)
;(write-xls "output.xlsx" data)


(defn data-to-csv-string [data]
  (let [writer (java.io.StringWriter.)]
    (csv/write-csv writer data)
    (.toString writer)))


;; Function to convert maps to CSV rows
(defn maps-to-csv [maps]
  (let [headers (keys (first maps))
        rows (map vals maps)]
    (cons headers rows)))

(defn keyword-to-title [k]
  (-> k
    name                            ; Convert keyword to string
    (str/replace "-" " ")           ; Replace hyphens with spaces
    (str/replace "_" " ")           ; Replace underscores with spaces (if applicable)
    (str/capitalize)))              ; Capitalize the first letter of each word

;; Function to convert maps to CSV rows with readable headers
(defn maps-to-csv [maps]
  (let [headers (map keyword-to-title (keys (first maps))) ; Convert keys to more readable header names
        rows (map vals maps)]
    (cons headers rows)))


(defn generate-excel-from-map [data-map]
  "Generates an Excel file from a map and returns a Java File object."
  (try
    (when (empty? data-map)
      (throw (IllegalArgumentException. "Data map cannot be empty")))
    (let [workbook (ss/create-workbook "Sheet1" [(map name (keys (first data-map)))])
          sheet (ss/select-sheet "Sheet1" workbook)]
      (doseq [row data-map]
        (let [row-values (map #(if (or (string? %) (number? %)) % (str %)) (vals row))]
          (ss/add-row! sheet row-values))) ;; Ensure values are strings or numbers
      (let [temp-file (doto (java.io.File/createTempFile "export" ".xlsx") (.deleteOnExit))] ;; Use java.io.File to create a temp file and ensure it is deleted on exit
        (with-open [output-stream (io/output-stream temp-file)]
          (ss/save-workbook! output-stream workbook)) ;; Save workbook using an output stream
        temp-file))
    (catch Exception e
      (log/error e "Failed to generate Excel from map")
      (throw e)))) ;; Rethrow the exception after logging

(defn excel-handler [request]
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

(defn get-export-routes []
  ["/export"

   {:swagger {:conflicting true
              :tags ["Export"] :security []}}

   ["/csv" {:get {:conflicting true
               :description (str "(DEV) |"
                              "- Exports csv/xls")
               ;:accept "application/json"
               :accept "text/csv"
               :coercion reitit.coercion.schema/coercion
               ;:middleware [accept-json-middleware
               ;             ;session/wrap
               ;             ]
               :swagger {:produces [
                                    ;"application/json"
                                    "text/csv"
                                    ]}

               ;:parameters {:body
               ;             {
               ;              :data [s/Any]
               ;              ;(s/optional-key :size) s/Int
               ;              }
               ;             }



               :handler (fn [request]
                          (let [output-stream (java.io.ByteArrayOutputStream.)
                                csv-data (maps-to-csv data)]
                            (with-open [writer (io/writer output-stream)]
                              (csv/write-csv writer csv-data))

                            {:status 200
                             :headers {"Content-Type" "text/csv"
                                       "Content-Disposition" "attachment; filename=\"output.csv\""}
                             :body (java.io.ByteArrayInputStream. (.toByteArray output-stream))}))


               :responses {200 {:description "OK"
                                :body s/Any}
                           404 {:description "Not Found"}
                           500 {:description "Internal Server Error"}}

               }}]









   ["/excel"
    {:get {:summary "Generate an Excel file from a map"
           :accept "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
           :coercion reitit.coercion.schema/coercion
           :swagger {:produces ["application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"]}
           :handler excel-handler}}]])
