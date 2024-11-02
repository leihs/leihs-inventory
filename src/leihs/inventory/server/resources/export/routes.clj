(ns leihs.inventory.server.resources.export.routes
  (:require
   [clojure.set]

   [ring.util.response :as response]

   [clojure.string :as str]
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



;; Example usage
(def data [{:name "Alice" :age 30} {:name "Bob" :age 25}])


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

(defn get-export-routes []
  [""
   ["/export"
    {:swagger {:conflicting true
               :tags ["Export"] :security []}}

    ["" {:post {:conflicting true
               :description (str "(DEV) |"
                                 "- Exports csv/xls")
               :accept "text/csv"
               :coercion reitit.coercion.schema/coercion
               :swagger {:produces [
                                    ;"application/json"
                                    "text/csv"
                                    "text/xls"
                                    ]}

               :parameters {:body
                            {
                             :data [s/Any]
                             }
                            }


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

                }}]]

  ])
