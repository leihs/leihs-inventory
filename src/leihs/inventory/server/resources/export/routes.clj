(ns leihs.inventory.server.resources.export.routes
  (:require
   [clojure.set]

   [ring.util.response :as response]


   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   ;[dk.ative.docjure.spreadsheet :as spreadsheet]

   ;[clojure.data.csv :as csv]
   ;[clojure.java.io :as io]

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
   [schema.core :as s]))






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

(defn get-export-routes []
  [""
   ["/export"
    {:swagger {:conflicting true
               :tags ["Export"] :security []}}

    ["" {:post {:conflicting true
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

               :parameters {:body
                            {
                             :data [s/Any]
                                    ;(s/optional-key :size) s/Int
                             }
                            }



                :handler (fn [request]
                           (let [output-stream (java.io.ByteArrayOutputStream.)]
                             (with-open [writer (io/writer output-stream)]
                               (csv/write-csv writer data))
                             {:status 200
                              :headers {"Content-Type" "text/csv"
                                        "Content-Disposition" "attachment; filename=\"output.csv\""}
                              :body (java.io.ByteArrayInputStream. (.toByteArray output-stream))}))



                ;:handler (fn [request]
                ;
                ;             {:status 200
                ;                                ;:headers {"Content-Type" "application/json"}
                ;                                :headers {"Content-Type" "text/csv"
                ;                                          "Content-Disposition" "attachment; filename=\"output.csv\""
                ;
                ;                                          }
                ;                                :body
                ;                                ;(write-csv "output.csv" data)
                ;              ;(data-to-csv-string data)
                ;
                ;
                ;              (io/piped-output-stream
                ;                      (fn [output-stream]
                ;                        (with-open [writer (io/writer output-stream)]
                ;
                ;                          (csv/write-csv writer data)
                ;
                ;                          ;(data-to-csv-string data)
                ;                          )))
                ;
                ;
                ;                                })












               :responses {200 {:description "OK"
                                :body s/Any}
                           404 {:description "Not Found"}
                           500 {:description "Internal Server Error"}}

                }}]]

  ])
